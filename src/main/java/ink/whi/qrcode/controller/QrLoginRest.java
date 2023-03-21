package ink.whi.qrcode.controller;

import com.github.hui.quick.plugin.qrcode.helper.QrCodeGenerateHelper;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeGenWrapper;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeOptions;
import com.google.zxing.WriterException;
import ink.whi.qrcode.utils.IpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@CrossOrigin
@Controller
public class QrLoginRest {

    @Value("${server.port}")
    private int port;

    @GetMapping(path = "login")
    public String qr(Map<String, Object> data) throws IOException, WriterException {
        String id = UUID.randomUUID().toString();   // 生成信息的唯一ID
        String ip = IpUtils.getLocalIP();

        String pref = "http://" + ip + ":" + port + "/";
        data.put("redirect", pref + "home");    // 二维码登录成功后跳转的页面
        data.put("subscribe", pref + "subscribe?id=" + id);    // 订阅URL, 用于推送消息

        String qrUrl = pref + "scan?id=" + id;  // 二维码中的URL
        String qrCode = QrCodeGenWrapper.of(qrUrl).setW(200).setDrawPreColor(Color.RED)
                .setDrawStyle(QrCodeOptions.DrawStyle.CIRCLE).asString();
        data.put("qrcode", qrCode);
        return "login";
    }

    private Map<String, SseEmitter> cache = new ConcurrentHashMap<>();

    /**
     * SseEmitter: 用于推送消息的对象
     * @param id
     * @return
     */
    @GetMapping(path = "subscribe", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter subscribe(String id) {
        SseEmitter sseEmitter = new SseEmitter(5 * 60 * 1000L);
        cache.put(id, sseEmitter);

        sseEmitter.onTimeout(() -> cache.remove(id));
        sseEmitter.onError((e) -> cache.remove(id));
        return sseEmitter;
    }

    /**
     * 用户扫码
     * @param model
     * @param request
     * @throws IOException
     */
    @GetMapping("scan")
    public String scan(Model model, HttpServletRequest request) throws IOException {
        String id = request.getParameter("id");
        SseEmitter sseEmitter = cache.get(id);
        if (sseEmitter != null) {
            // 向pc推送已扫码消息
            sseEmitter.send("scan");
        }

        // 授权
        String url = "http://" + IpUtils.getLocalIP() + ":" + port + "/accept?id=" + id;
        model.addAttribute("url", url);
        return "scan";
    }

    @ResponseBody
    @GetMapping("accept")
    public String accept(String id, String token) throws IOException {
        SseEmitter sseEmitter = cache.get(id);
        if (sseEmitter != null) {
            // 保存token, 并返回Cookie
            // fixme: 这里的token还需要校验
            sseEmitter.send("login#cookie" + token);
            sseEmitter.complete();
            cache.remove(id);
        }

        return "登录成功: " + token;   // 注意：这个是返回给app端的信息
    }

    @GetMapping(path = {"home", ""})
    @ResponseBody
    public String home(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return "未登录！";
        }
        // fixme: 这里实现的不太优雅
        Optional<Cookie> cookie = Stream.of(cookies).filter(s -> s.getName().equalsIgnoreCase("qrlogin")).findFirst();
        return cookie.map(cookie1 -> "欢迎进入首页: " + cookie1.getValue()).orElse("未登录");
    }
}
