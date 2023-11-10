package com.chef.assist.controller;

import com.chef.assist.mapper.RoleMapper;
import com.chef.assist.mapper.UserMapper;
import com.chef.assist.model.Role;
import com.chef.assist.model.User;
import com.chef.assist.model.dto.UserVO;
import com.chef.assist.utils.MyStringUtil;
import com.chef.assist.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/sample")
public class SampleController {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserMapper userMapper;
    private final SessionRegistry sessionRegistry;

    @Autowired
    public SampleController(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }
    @PostMapping("/fake-reg")
    public void fakeReg() throws InvalidKeySpecException, NoSuchAlgorithmException {
        User user = new User();
        Role role = roleMapper.findByRoleName("管理员");
        user.setRoleId(role.getId());
        user.setUsername("admin");
        String salt= MyStringUtil.randomAlphaNumeric(10);;
        user.setSalt(salt);
        user.setPassword(SecurityUtil.generateStorngPasswordHash("admin", salt));
        userMapper.insert(user);
    }

    @GetMapping("/user")
    public UserVO getUser() {
        UserVO result = new UserVO();
        result.setUsername("sample");
        return result;
    }
    @GetMapping("/active-users")
    public List<String> getActiveUsers() {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        List<String> activeUsersWithSessionId = new ArrayList<>();

        for (Object principal : principals) {
            List<SessionInformation> sessionInformationList = sessionRegistry.getAllSessions(principal, false);

            for (SessionInformation sessionInformation : sessionInformationList) {
                // 判断会话是否过期
                if (!sessionInformation.isExpired()) {
                    String username = sessionInformation.getPrincipal().toString();
                    String sessionId = sessionInformation.getSessionId();
                    activeUsersWithSessionId.add("User: " + username + ", Session ID: " + sessionId);
                }
            }
        }

        return activeUsersWithSessionId;
    }
}
