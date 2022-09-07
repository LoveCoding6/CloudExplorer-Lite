package com.fit2cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit2cloud.base.entity.Role;
import com.fit2cloud.base.entity.User;
import com.fit2cloud.base.entity.UserRole;
import com.fit2cloud.common.constants.RoleConstants;
import com.fit2cloud.common.constants.SystemUserConstants;
import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.utils.CurrentUserUtils;
import com.fit2cloud.common.utils.MD5Util;
import com.fit2cloud.constants.ErrorCodeConstants;
import com.fit2cloud.dao.mapper.UserMapper;
import com.fit2cloud.dto.RoleInfo;
import com.fit2cloud.dto.UserDto;
import com.fit2cloud.dto.UserOperateDto;
import com.fit2cloud.dto.UserRoleDto;
import com.fit2cloud.request.CreateUserRequest;
import com.fit2cloud.request.PageUserRequest;
import com.fit2cloud.service.IUserService;
import com.fit2cloud.service.OrganizationCommonService;
import com.fit2cloud.service.WorkspaceCommonService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author fit2cloud
 * @since
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    RoleServiceImpl roleServiceImpl;

    @Resource
    BaseMapper<UserRole> userRoleMapper;

    @Resource
    BaseMapper<Role> roleMapper;

    @Resource
    OrganizationCommonService organizationCommonService;

    @Resource
    WorkspaceCommonService workspaceCommonService;

    @Resource
    private MessageSource messageSource;

    @Override
    public IPage<UserDto> pageUser(PageUserRequest pageUserRequest) {
        Page<User> page = new Page<>(pageUserRequest.getCurrentPage(), pageUserRequest.getPageSize());
        page.setOrders(new ArrayList<>() {{
            add(pageUserRequest.getOrder());
        }});

        QueryWrapper<User> wrapper = new QueryWrapper<>();

        if (StringUtils.isNotEmpty(pageUserRequest.getUsername())) {
            wrapper.like(true, "username", pageUserRequest.getUsername());
        }
        if (StringUtils.isNotEmpty(pageUserRequest.getName())) {
            wrapper.like(true, "user._name", pageUserRequest.getName());
        }
        if (StringUtils.isNotEmpty(pageUserRequest.getEmail())) {
            wrapper.like(true, "email", pageUserRequest.getEmail());
        }
        if (StringUtils.isNotEmpty(pageUserRequest.getRoleId())) {
            wrapper.eq(true, "role_id", pageUserRequest.getRoleId());
        }

        IPage<User> userIPage = baseMapper.pageUser(page, wrapper);

        Map<String, Object> param = new HashMap<>();
        return userIPage.convert(user -> {
            param.put("userId", user.getId());
            UserDto userDto = new UserDto();
            userDto.setRoles(roleServiceImpl.getRolesByResourceIds(param));
            BeanUtils.copyProperties(user, userDto);
            return userDto;
        });
    }

    @Override
    public boolean deleteUser(String userId) {
        //TODO 获取当前登录用户ID,用户角色
        String currentLoginUserId = "admin";
        String currentLoginUserRole = "ADMIN";

        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new Fit2cloudException(ErrorCodeConstants.USER_NOT_EXIST.getCode(), ErrorCodeConstants.USER_CAN_NOT_DELETE.getMessage());
        }

        if (StringUtils.equalsIgnoreCase(user.getUsername(), currentLoginUserId)) {
            throw new Fit2cloudException(ErrorCodeConstants.USER_CAN_NOT_DELETE.getCode(), ErrorCodeConstants.USER_CAN_NOT_DELETE.getMessage());
        }

        // TODO 增加删除用户的操作日志
        System.out.println(user.getUsername() + "is deleted by" + currentLoginUserId);

        // 当前登录用户以组织管理员的角色删除用户
        if (currentLoginUserRole.equalsIgnoreCase(RoleConstants.ROLE.ORGADMIN.name())) {
            // 当为组织管理员时，删除当前组织及其子组织下的工作空间（解绑关系user_role）
            List<String> sourceList = new ArrayList<>();

            // TODO 获取当前组织及其所有子组织
            List<String> subOrgIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(subOrgIds)) {
                sourceList.addAll(subOrgIds);
            }

            // TODO 查询当前组织及其子组织下的工作空间
            List<String> workspaceIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(workspaceIds)) {
                sourceList.addAll(workspaceIds);
            }

            //TODO 根据sourceList，删除用户角色记录
            QueryWrapper<UserRole> userRoleWrapper = Wrappers.query();
            userRoleWrapper.eq(true, "id", userId).in(true, "_source", sourceList);
            userRoleMapper.delete(userRoleWrapper);

            //TODO 查询该用户具有的所有角色，如果为空，则删除用户
            List<UserRoleDto> userRoleDtoList = new ArrayList<>();
            if (CollectionUtils.isEmpty(userRoleDtoList)) {
                baseMapper.deleteById(userId);
            }
        }

        // 当前登录用户以系统管理员的角色删除用户
        if (currentLoginUserRole.equalsIgnoreCase(RoleConstants.ROLE.ADMIN.name())) {
            // 删除用户
            baseMapper.deleteById(userId);

            // 删除用户的角色关系
            QueryWrapper<UserRole> userRoleWrapper = Wrappers.query();
            userRoleWrapper.eq(true, "user_id", userId);
            userRoleMapper.delete(userRoleWrapper);
        }
        return true;
    }

    public boolean changeUserStatus(UserDto userDto) {
        User userUpdate = new User();
        userUpdate.setId(userDto.getId());
        userUpdate.setEnabled(userDto.getEnabled());
        if (userDto.getRoles().stream().map(Role::getId).anyMatch(s -> s.equals(RoleConstants.ROLE.ADMIN.name())) && !userDto.getEnabled()) {
            Long countAdmin = baseMapper.countActiveUsers(RoleConstants.ROLE.ADMIN.name());
            if (countAdmin > 1) {
                baseMapper.updateById(userUpdate);
            } else {
                throw new Fit2cloudException(ErrorCodeConstants.USER_KEEP_ONE_ADMIN.getCode(),  ErrorCodeConstants.USER_KEEP_ONE_ADMIN.getMessage());
            }
        } else {
            baseMapper.updateById(userUpdate);
        }
        return true;
    }

    public boolean createUser(CreateUserRequest request) {
        validateUserParam(request);
        UserOperateDto user = new UserOperateDto();
        BeanUtils.copyProperties(request, user);
        insertUser(user);
        return true;
    }

    @Transactional
    public boolean updateUser(CreateUserRequest request) {
        // 校验用户邮箱是否已存在
        validateUserDataExist("email", request.getEmail(), "邮箱","update",request.getId());

        UserOperateDto user = new UserOperateDto();
        BeanUtils.copyProperties(request, user);
        baseMapper.updateById(user);
        QueryWrapper<UserRole> wrapper = Wrappers.query();
        wrapper.lambda().eq(true, UserRole::getUserId, request.getId());

        if (CollectionUtils.isNotEmpty(user.getRoleInfoList())) {
            if (CurrentUserUtils.isAdmin()) {
                // 删除要编辑的用户在 user_role 的信息，然后 reinsert
                userRoleMapper.delete(wrapper);
            }

            if (CurrentUserUtils.isOrgAdmin()) {
                // 删除要编辑的用户在当前组织下的 user_role 的信息，然后 reinsert
                List<String> list = new ArrayList<>();
                List<String> orgIds = organizationCommonService.getOrgIdsByPid(CurrentUserUtils.getOrganizationId());
                list.addAll(orgIds);
                List<String> workspaceIds = workspaceCommonService.getWorkspaceIdsByOrgIds(list);
                if (CollectionUtils.isNotEmpty(workspaceIds)) {
                    list.addAll(workspaceIds);
                }
                wrapper.lambda().in(true, UserRole::getSource, list);
                userRoleMapper.delete(wrapper);
            }

            insertUserRoleInfo(user);
        }
        return true;
    }

    public boolean updatePwd(User user) {
        // 非本地创建用户不允许修改密码
        if (!"local".equalsIgnoreCase(CurrentUserUtils.getUser().getSource())) {
            throw new Fit2cloudException(ErrorCodeConstants.USER_KEEP_ONE_ADMIN.getCode(), ErrorCodeConstants.USER_KEEP_ONE_ADMIN.getMessage());
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(MD5Util.md5(user.getPassword()));
        baseMapper.updateById(updateUser);
        return true;
    }

    @Transactional
    public boolean insertUser(UserOperateDto user) {
        if (StringUtils.isBlank(user.getId())) {
            user.setId(UUID.randomUUID().toString());
        }
        user.setPassword(MD5Util.md5(user.getPassword()));
        baseMapper.insert(user);
        if (CollectionUtils.isNotEmpty(user.getRoleInfoList())) {
            insertUserRoleInfo(user);
        }
        return true;
    }

    public void insertUserRoleInfo(UserOperateDto user) {
        for (RoleInfo roleInfo : user.getRoleInfoList()) {
            UserRole userRole = new UserRole();
            userRole.setRoleId(roleInfo.getRoleId());
            userRole.setUserId(user.getId());

            RoleConstants.ROLE parentRoleId = getParentRoleId(roleInfo.getRoleId());
            if (RoleConstants.ROLE.USER.equals(parentRoleId)) {
                roleInfo.getWorkspaceIds().forEach(workspaceId -> insertUserRoleInfo(userRole, workspaceId));
            }
            if (RoleConstants.ROLE.ORGADMIN.equals(parentRoleId)) {
                roleInfo.getOrganizationIds().forEach(organizationId -> insertUserRoleInfo(userRole, organizationId));
            }
            if (RoleConstants.ROLE.ADMIN.equals(parentRoleId)) {
                insertUserRoleInfo(userRole, null);
            }
        }
    }

    public UserOperateDto userRoleInfo(String userId) {
        // 查询角色信息
        List roleInfos = roleInfo(userId);

        // 查询用户信息
        User user = baseMapper.selectById(userId);
        UserOperateDto userOperateDto = new UserOperateDto();

        // 组合数据
        BeanUtils.copyProperties(user, userOperateDto);
        userOperateDto.setPassword("");
        userOperateDto.setRoleInfoList(roleInfos);

        return userOperateDto;
    }

    public List<RoleInfo> roleInfo(String userId) {
        Map<String, Object> param = new HashMap<>();
        param.put("userId", userId);
        if (CurrentUserUtils.isOrgAdmin()) {
            List<String> orgIds = organizationCommonService.getOrgIdsByPid(CurrentUserUtils.getOrganizationId());
            List<String> resourceIds = workspaceCommonService.getWorkspaceIdsByOrgIds(orgIds);
            resourceIds.addAll(orgIds);
            param.put("resourceIds", resourceIds);
        }
        return baseMapper.roleInfo(param);
    }

    private void insertUserRoleInfo(UserRole userRole, String sourceId) {
        userRole.setId(UUID.randomUUID().toString());
        userRole.setSource(sourceId);
        userRoleMapper.insert(userRole);
    }

    public RoleConstants.ROLE getParentRoleId(String roleId) {
        Role role = roleMapper.selectById(roleId);
        return role.getParentRoleId();
    }

    /**
     * 校验用户参数
     *
     * @param request
     */
    private void validateUserParam(CreateUserRequest request) {
        if (StringUtils.isBlank(request.getUsername())) {
            throw new RuntimeException("用户ID不能为空");
        }

        if (StringUtils.isBlank(request.getName())) {
            throw new RuntimeException("用户名不能为空");
        }

        if (StringUtils.isBlank(request.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }

        if (StringUtils.isBlank(request.getEmail())) {
            throw new RuntimeException("邮箱不能为空");
        }

        if (SystemUserConstants.getUserName().equalsIgnoreCase(request.getUsername())) {
            throw new RuntimeException("用户ID不能为system");
        }

        // 校验用户ID是否已存在
        validateUserDataExist("username", request.getUsername(), "用户ID","create",null);

        // 校验用户邮箱是否已存在
        validateUserDataExist("email", request.getEmail(), "邮箱","create",null);
    }

    /**
     * 校验用户某属性值是否已存在
     *
     * @param colName
     * @param colValue
     * @param colDisplayName
     */
    private void validateUserDataExist(String colName, String colValue, String colDisplayName,String optType,String id) {
        QueryWrapper<User> wrapper = Wrappers.query();
        wrapper.eq(true, colName, colValue);
        if ("created".equals(optType) && baseMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException(colDisplayName + "已存在");
        }
        if ("update".equals(optType)) {
            wrapper.ne(true, "id", id);
            if (baseMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException(colDisplayName + "已存在");
            }
        }
    }
}
