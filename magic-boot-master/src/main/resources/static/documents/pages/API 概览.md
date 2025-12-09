# API 概览
                
本文档提供我们产品API的概览信息。

## 基础信息

- **API版本**: v1.0
- **基础URL**: https://api.example.com/v1
- **数据格式**: JSON

## 认证方式

所有API请求都需要在HTTP头部包含有效的API密钥：

```
Authorization: Bearer your-api-key
```

## 通用响应格式

所有API响应都遵循以下格式：

```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "code": 200
}
```

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |`,

'auth': `# 认证授权 API
                
本文档描述认证和授权相关的API接口。

## 用户登录

```
POST /auth/login
```

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 123,
      "username": "admin",
      "email": "admin@example.com"
    }
  },
  "message": "登录成功",
  "code": 200
}
```

## 刷新令牌

```
POST /auth/refresh
```

使用刷新令牌获取新的访问令牌。