package com.fit2cloud.common.log.cache;

import com.fit2cloud.base.entity.Role;
import com.fit2cloud.base.mapper.BaseRoleMapper;
import com.fit2cloud.common.utils.SpringUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.lang3.StringUtils;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 云主机ID名称缓存
 * {@code @Author:张少虎}
 * {@code @Date: 2022/11/1  4:29 PM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class BaseRoleCache {
    /**
     * 云账号id -> name的缓存 使用软引用,在虚拟机内存不足的情况下会被清除
     */
    private static SoftReference<Map<String, String>> roleCache = new SoftReference<>(new HashMap<>());

    private synchronized static void setCache(Map<String, String> cacheData) {
        roleCache = new SoftReference<>(cacheData);
    }

    public static void updateCache() {
        BaseRoleMapper mapper = SpringUtil.getBean(BaseRoleMapper.class);
        List<Role> list = mapper.selectList(null);
        Map<String, String> cacheData = list.stream().map(w -> {
            return new DefaultKeyValue<>(w.getId(), w.getName());
        }).collect(Collectors.toMap(DefaultKeyValue::getKey, DefaultKeyValue::getValue));
        setCache(cacheData);
    }

    public synchronized static String getCache(String id) {
        Map<String, String> map = roleCache.get();
        return MapUtils.isEmpty(map) ? null : map.get(id);
    }

    public synchronized static String getCacheOrUpdate(String id) {
        String cache = getCache(id);
        if (StringUtils.isEmpty(cache)) {
            BaseRoleMapper mapper = SpringUtil.getBean(BaseRoleMapper.class);
            Role vo = mapper.selectById(id);
            if (Objects.nonNull(vo)) {
                updateCache();
                return vo.getName();
            }
        }
        return cache;

    }
}
