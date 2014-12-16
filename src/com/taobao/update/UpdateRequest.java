package com.taobao.update;


/**
 * 更新服务端请求
 * @author bokui
 *
 */
public interface UpdateRequest {
	/**
	 * 请求服务端
	 * @param appName	应用名
	 * @param version	本地应用版本
	 * @param appMd5	本地应用md5
	 * @return	更新结果
	 */
	public UpdateInfo request(String appName, String version, String appMd5);
}
