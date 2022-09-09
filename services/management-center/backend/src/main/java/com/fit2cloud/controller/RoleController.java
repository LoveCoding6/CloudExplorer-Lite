package com.fit2cloud.controller;

import com.fit2cloud.base.entity.Role;
import com.fit2cloud.base.mapper.BaseRoleMapper;
import com.fit2cloud.common.validator.annnotaion.CustomValidated;
import com.fit2cloud.common.validator.group.ValidationGroup;
import com.fit2cloud.common.validator.handler.ExistHandler;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.controller.request.role.CreateRoleRequest;
import com.fit2cloud.controller.request.role.UpdateRoleRequest;
import com.fit2cloud.service.IRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;


@RestController
@RequestMapping("/api/role")
@Api("角色相关接口")
public class RoleController {

    @Resource
    IRoleService roleService;

    @ApiOperation(value = "添加角色", notes = "添加角色")
    @PreAuthorize("hasAnyCePermission('ROLE:CREATE')")
    @PostMapping
    public ResultHolder<Role> save(@RequestBody @Validated(ValidationGroup.SAVE.class) CreateRoleRequest request) {
        Role role = new Role();
        BeanUtils.copyProperties(request, role);
        roleService.save(role);
        return ResultHolder.success(roleService.getById(role.getId()));
    }

    @ApiOperation(value = "更新角色", notes = "更新角色")
    @PreAuthorize("hasAnyCePermission('ROLE:EDIT')")
    @PutMapping
    public ResultHolder<Role> update(@RequestBody @Validated(ValidationGroup.UPDATE.class) UpdateRoleRequest request) {
        Role role = new Role();
        BeanUtils.copyProperties(request, role);
        roleService.updateById(role);
        return ResultHolder.success(roleService.getById(role.getId()));
    }

    @ApiOperation(value = "删除组织", notes = "删除组织")
    @PreAuthorize("hasAnyCePermission('ROLE:DELETE')")
    @DeleteMapping
    public ResultHolder<Boolean> removeRole(
            @ApiParam("角色ID")
            @NotNull(message = "角色ID不能为空")
            @CustomValidated(mapper = BaseRoleMapper.class, handler = ExistHandler.class, message = "角色ID不存在", exist = false)
            @RequestParam("id")
            String id) {
        return ResultHolder.success(roleService.deleteRoleById(id));
    }


}
