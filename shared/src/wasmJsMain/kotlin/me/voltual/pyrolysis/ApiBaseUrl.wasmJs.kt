package me.voltual.pyrolysis

actual val DefaultApiBaseUrl: String = "https://api.smallfun.cn/"
// WasmJS 端通过 Worker 同源反代路由，绕过浏览器跨域与限制
actual val DefaultLanzouBaseUrl: String = "/lanyun"
actual val DefaultSmallfunFileBaseUrl: String = "/smallfun-file/"