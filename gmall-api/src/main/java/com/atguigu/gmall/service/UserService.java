package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;

import java.util.List;

public interface UserService {
    List<UmsMember> getAllUser();

    UmsMember getUserById(String id);
}
