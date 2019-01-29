package io.kurumi.ntt;

import cn.hutool.core.lang.caller.*;
import cn.hutool.log.*;
import com.pengrad.telegrambot.*;
import com.pengrad.telegrambot.request.*;
import io.kurumi.ntt.ui.*;
import io.kurumi.ntt.webhookandauth.*;
import java.io.*;
import java.lang.reflect.*;

public class BotMain {

    public static final String version = "0.2";

    public Log log;

    public TelegramBot bot;
    public MainAdapter adapter;

    public Data data;

    public BotMain(File rootDir) {

        log = StaticLog.get("NTTBot");

        log.info("NTTBot 正在启动 版本 : " + version);

        Constants.data = this.data = new Data(rootDir);

    

        adapter = new MainAdapter(this);

        if (data.botToken == null) {

            Setup.start();

        }

        Constants.bot = bot = new TelegramBot(data.botToken);

        bot.execute(new DeleteWebhook());

        Constants.authandwebhook = new ServerManager();

        if (data.useServer && Constants.authandwebhook.initServer(data.serverPort, data.serverDomain)) {

            log.info("服务器启动成功..");

            bot.setUpdatesListener(adapter, new GetUpdates());
            
            bot.execute(new DeleteWebhook());
           
          //  bot.execute(new SetWebhook().url("https://" + data.serverDomain + "/" + data.botToken).allowedUpdates(allows), cb);
   
        } else {

            log.error("服务器启动失败...");

        }

    }

    public static void main(String[] args) {

        new BotMain(new File("."));

    }

}
