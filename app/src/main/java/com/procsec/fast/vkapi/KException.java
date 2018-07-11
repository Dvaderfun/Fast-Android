package com.procsec.fast.vkapi;

@SuppressWarnings("serial")
public class KException extends Exception {
    public int error_code;
    public String url;

    public String captcha_img;
    public String captcha_sid;

    public String redirect_uri;

    public KException(int code, String message, String url) {
        super(message);
        error_code = code;
        this.url = url;
    }
}
