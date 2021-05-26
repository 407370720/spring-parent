package com.yaomy.security.oauth2.authority;

import com.emily.infrastructure.common.utils.json.JSONUtils;
import com.google.common.collect.Maps;
import org.springframework.security.core.GrantedAuthority;

import java.util.Map;

/**
 * @Description: 自定义GrantedAuthority接口
 * @ProjectName: spring-parent
 * @Package: com.yaomy.security.oauth2.authority.UserGrantedAuthority
 * @Date: 2019/7/29 16:14
 * @Version: 1.0
 */
public class UserGrantedAuthority implements GrantedAuthority {
    private Map<String, Object> authoritys = Maps.newHashMap();
    public UserGrantedAuthority(String name, Object value){
        authoritys.put(name,value);
    }
    @Override
    public String getAuthority() {
        return JSONUtils.toJSONString(authoritys);
    }
}
