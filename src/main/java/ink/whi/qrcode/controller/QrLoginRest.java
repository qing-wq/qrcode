package ink.whi.qrcode.controller;

import com.github.hui.quick.plugin.qrcode.helper.QrCodeGenerateHelper;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeGenWrapper;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeOptions;
import com.google.zxing.WriterException;
import ink.whi.qrcode.utils.IpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import javax.validation.Valid;
import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
        data.put("subscribe", pref + "subscribe?id=" + id);    // 订阅URL

        String qrUrl = pref + "scan?id=" + id;
        String qrCode = QrCodeGenWrapper.of(qrUrl).setW(200).setDrawPreColor(Color.RED)
                .setDrawStyle(QrCodeOptions.DrawStyle.CIRCLE).asString();
        data.put("qrcode", qrCode);
        return "login";
    }
}
