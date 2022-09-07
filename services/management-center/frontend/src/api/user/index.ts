import { get, post, del } from "@commons/request";
import type Result from "@commons/request/Result";
import type { Page } from "@commons/request/Result";
import type {
  ListUserRequest,
  CreateUserRequest,
  User,
  UpdateUserPwdRequest,
  UpdateUserStatusRequest,
} from "./type";

export const listUser: (req: ListUserRequest) => Promise<Result<Page<User>>> = (
  req
) => {
  return get("/api/user/page", req);
};

export const createUser = (req: CreateUserRequest) => {
  return post("/api/user/add", "", req);
};

export const updateUser = (req: CreateUserRequest) => {
  return post("/api/user/update", "", req);
};

export const updatePwd = (req: UpdateUserPwdRequest) => {
  return post("/api/user/updatePwd", "", req);
};

export const deleteUserById = (userId: string) => {
  return del("/api/user/" + userId);
};

/**
 * 获取所有角色
 */
export const listCurrentUserRole = () => {
  return get("/api/role/current");
};

export const getRoleInfo = (userId: string) => {
  return get("/api/user/role/info/" + userId);
};

export const workspaceTree = (): Promise<Result<Array<any>>> => {
  return get("/api/base/workspace/tree");
};

/**
 * 启停用户
 * @param req
 */
export const changeUserStatus = (req: UpdateUserStatusRequest) => {
  return post("/api/user/changeStatus", "", req);
};
