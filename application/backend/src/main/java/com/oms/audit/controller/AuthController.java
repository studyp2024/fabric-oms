package com.oms.audit.controller;

import com.oms.audit.entity.User;
import com.oms.audit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 认证与用户管理控制器
 * 提供用户登录、注册、查询及删除的 RESTful API 接口
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // 允许跨域请求
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户登录接口
     *
     * @param loginRequest 包含用户名和密码的请求体
     * @return 登录成功返回用户信息，失败返回 401 状态码和提示信息
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> user = userRepository.findByUsername(loginRequest.getUsername());
        // 简单明文密码比对（在实际生产环境中应使用哈希加密密码）
        if (user.isPresent() && user.get().getPassword().equals(loginRequest.getPassword())) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.status(401).body("Invalid credentials"); // 认证失败
    }

    /**
     * 用户注册接口
     *
     * @param user 包含用户注册信息的请求体
     * @return 注册成功返回保存的用户信息，用户名已存在返回 400 状态码
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        // 检查用户名是否已经被注册
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        return ResponseEntity.ok(userRepository.save(user));
    }

    /**
     * 获取所有用户列表
     *
     * @return 包含所有用户信息的列表
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    /**
     * 根据用户 ID 删除指定用户
     *
     * @param id 待删除用户的唯一标识
     * @return 删除成功返回 200 状态码，用户不存在返回 404 状态码
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
