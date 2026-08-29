# -*- coding: utf-8 -*-
"""按官方 cos-js-sdk-v5 的算法算一遍签名，用来核对 Kotlin 实现。

算法是从站点自己加载的那份 SDK（mob_libs.js）里读出来的，不是从文档抄的：

    u  = ["cache-control","content-disposition","content-encoding",
          "content-length","content-md5","expect","expires","host",
          "if-match","if-modified-since","if-none-match",
          "if-unmodified-since","origin","range","transfer-encoding"]
    d  = 只保留 x-cos-* 和 u 白名单里的头
    m  = 头的键**排序**后用 ";" 连、小写        -> q-header-list
    q  = HmacSHA1(KeyTime, SecretKey)          -> SignKey
    L  = [method, path, 查询串, 头串, ""].join("\n")   -> HttpString
    y  = ["sha1", KeyTime, SHA1(L), ""].join("\n")     -> StringToSign
    W  = HmacSHA1(y, q)                        -> Signature

注意 CryptoJS 的 HmacSHA1(message, key) 是**消息在前**。
"""
import hashlib
import hmac
from urllib.parse import quote

WHITELIST = {
    "cache-control", "content-disposition", "content-encoding",
    "content-length", "content-md5", "expect", "expires", "host",
    "if-match", "if-modified-since", "if-none-match",
    "if-unmodified-since", "origin", "range", "transfer-encoding",
}


def p(v):
    """SDK 的 p()：encodeURIComponent + 再编 !'()* 。"""
    # encodeURIComponent 不编码 A-Za-z0-9 - _ . ! ~ * ' ( )
    # 先按它的规则编，再把 !'()* 也编掉，剩下 - _ . ~ 不编
    out = quote(str(v), safe="-_.!~*'()")
    for ch, enc in (("!", "%21"), ("'", "%27"), ("(", "%28"), (")", "%29"), ("*", "%2A")):
        out = out.replace(ch, enc)
    return out


def filter_headers(headers):
    return {k: v for k, v in headers.items()
            if k.lower().startswith("x-cos-") or k.lower() in WHITELIST}


def obj2str(d, lower_key):
    keys = sorted(d.keys(), key=lambda s: s.lower())
    parts = []
    for k in keys:
        v = "" if d[k] is None else str(d[k])
        ek = p(k).lower() if lower_key else p(k)
        parts.append("%s=%s" % (ek, p(v)))
    return "&".join(parts)


def key_list(d):
    return ";".join(sorted((p(k).lower() for k in d.keys()), key=lambda s: s.lower()))


def get_auth(secret_id, secret_key, method, pathname, key_time, headers, query=None):
    query = query or {}
    hdrs = filter_headers(headers)
    header_list = key_list(hdrs)
    param_list = key_list(query)

    sign_key = hmac.new(secret_key.encode(), key_time.encode(), hashlib.sha1).hexdigest()
    http_string = "\n".join([method.lower(), pathname, obj2str(query, True), obj2str(hdrs, True), ""])
    sha1_http = hashlib.sha1(http_string.encode()).hexdigest()
    string_to_sign = "\n".join(["sha1", key_time, sha1_http, ""])
    signature = hmac.new(sign_key.encode(), string_to_sign.encode(), hashlib.sha1).hexdigest()

    auth = "&".join([
        "q-sign-algorithm=sha1",
        "q-ak=" + secret_id,
        "q-sign-time=" + key_time,
        "q-key-time=" + key_time,
        "q-header-list=" + header_list,
        "q-url-param-list=" + param_list,
        "q-signature=" + signature,
    ])
    return {
        "SignKey": sign_key,
        "HttpString": http_string,
        "SHA1(HttpString)": sha1_http,
        "StringToSign": string_to_sign,
        "Signature": signature,
        "Authorization": auth,
    }


if __name__ == "__main__":
    # 固定的测试向量，Kotlin 那边用同样的输入应该逐字段一致
    r = get_auth(
        secret_id="AKIDTESTTESTTESTTESTTESTTEST",
        secret_key="SECRETKEYTESTTESTTESTTESTTEST",
        method="put",
        pathname="/posts/20260829/12345678/1756000000000_abcd1234.jpg",
        key_time="1756000000;1756003600",
        headers={"Host": "ff14risingstones.gcloud.com.cn", "Content-Type": "image/jpeg"},
    )
    for k in ("SignKey", "SHA1(HttpString)", "Signature"):
        print("%-18s %s" % (k, r[k]))
    print()
    print("HttpString   =", repr(r["HttpString"]))
    print("StringToSign =", repr(r["StringToSign"]))
    print()
    print("Authorization:")
    print(r["Authorization"])
