import { get, post } from "@commons/request";
import type { LoginRequest, LoginResponse, User } from "./type";
import type { Result } from "@commons/request/Result";
import type { Ref } from "vue";

export function login(
  data: LoginRequest,
  loading?: Ref<boolean>
): Promise<Result<LoginResponse>> {
  return post("/login", null, data, loading);
}

export function fetchCurrentUser(
  loading?: Ref<boolean>
): Promise<Result<User>> {
  return get("/api/user/current", null, loading);
}

export function getUser(id: string): Promise<Result<User>> {
  return get(`/api/user/${id}`);
}

export const saveUser = (data: User, loading?: Ref<boolean>) => {
  return post("/api/user-save", null, data, loading);
};

export const updateUserPwd = (data: any) => {
  return post("/api/user-pwd-update", null, data);
};

export const getApiKeys = () => {
  return get("/api/key");
};

export const createApiKeys = () => {
  return post("/api/key/create");
};

export const deleteApiKeys = (data: any) => {
  return post("/api/key/delete", null, data);
};

export const updateApiKeys = (data: any) => {
  return post("/api/key/update", null, data);
};
