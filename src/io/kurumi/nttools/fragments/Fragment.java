package io.kurumi.nttools.fragments;

import cn.hutool.core.util.StrUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.InlineQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.SetWebhook;
import io.kurumi.nttools.server.BotServer;
import io.kurumi.nttools.twitter.TwiAccount;
import io.kurumi.nttools.utils.CData;
import io.kurumi.nttools.utils.UserData;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class Fragment {

    public MainFragment main;
    public String token;
    public TelegramBot bot;

    public Fragment(MainFragment main) {

        this.main = main;

    }

    public abstract String name();

    public Fragment initBot() {

        this.token = main.tokens.get(name());
        this.bot = new TelegramBot(token);

        return this;

    }

    public void startGetUpdates() {

        deleteWebHook();

        bot.setUpdatesListener(new UpdatesListener() {
                @Override
                public int process(List<Update> updates) {

                    for (Update update : updates) {

                        processUpdate(update);

                    }

                    return CONFIRMED_UPDATES_ALL;
                }
            });

    }

    public void setWebHook() {

        BotServer.bots.put(token, this);

        bot.execute(new SetWebhook().url("https://" + main.serverDomain + "/" + token));

    }

    public void deleteWebHook() {

        bot.execute(new DeleteWebhook());

        BotServer.bots.remove(token);

    }

    public void processUpdate(Update update) {

        if (update.message() != null) {

            UserData user = getUser(update.message());

            switch (update.message().chat().type()) {

                    case Private : processPrivateMessage(user, update.message());return;
                    case group : processGroupMessage(user, false, update.message());return;
                    case supergroup : processGroupMessage(user, true, update.message());return;

            }

        } else if (update.channelPost() != null) {

            UserData user = getUser(update.message().from());

            processChannelPost(user, update.channelPost());

        } else if (update.callbackQuery() != null) {

            UserData user = getUser(update.callbackQuery().from());

            processCallbackQuery(user, update.callbackQuery());

        } else if (update.inlineQuery() != null) {

            UserData user = getUser(update.inlineQuery().from());

            processInlineQuery(user, update.inlineQuery());

        } else if (update.chosenInlineResult() != null) {

            UserData user = getUser(update.chosenInlineResult().from());

            processChosenInlineQueryResult(user, update.inlineQuery());

        }

    }

    public void processPrivateMessage(UserData user, Message msg) {}
    public void processGroupMessage(UserData user, boolean superGroup, Message msg) {}
    public void processChannelPost(UserData user, Message msg) {}
    public void processCallbackQuery(UserData user, CallbackQuery callbackQuery) {}
    public void processInlineQuery(UserData user, InlineQuery inlineQuery) {}
    public void processChosenInlineQueryResult(UserData user, InlineQuery inlineQuery) {}


    private HashMap<Long,UserData> userDataCache = new HashMap<>();

    public LinkedList<UserData> loadUsers() {

        for (File userDataFile : new File(main.dataDir, name() + "/users").listFiles()) {

            Long userId = Long.parseLong(StrUtil.subBefore(userDataFile.getName(), ".json", true));

            if (userDataCache.containsKey(userId)) continue;

            userDataCache.put(userId, new UserData(this, userId));

        }

        return getUsers();

    }

    public LinkedList<UserData> getUsers() {

        return new LinkedList<UserData>(userDataCache.values());

    }

    public UserData getUser(Message msg) {

        UserData userData = getUser(msg.from().id());

        userData.update(msg);

        userData.save();

        return userData;

    }

    public UserData getUser(User user) {

        UserData userData = getUser(user.id());

        userData.update(user);

        userData.save();

        return userData;

    }

    public UserData getUser(long id) {

        if (userDataCache.containsKey(id)) return userDataCache.get(id);

        UserData userData = new UserData(this, id);

        userDataCache.put(id, userData);

        return userData;

    }

    public static boolean isCommand(Message msg) {

        if (msg.text() == null) return false;

        return msg.text().startsWith("/");

    }

    public static String getCommandName(Message msg) {

        if (msg.text() == null) return null;

        if (!msg.text().contains("/")) return null;

        String body = StrUtil.subAfter(msg.text(), "/", false);

        if (body.contains(" ")) {

            String cmdAndUser =  StrUtil.subBefore(body, " ", false);

            if (cmdAndUser.contains("@")) {

                return StrUtil.subBefore(cmdAndUser, "@", false);

            } else {

                return cmdAndUser;

            }

        } else if (body.contains("@")) {

            return StrUtil.subBefore(body, "@", false);

        } else {

            return body;

        }

    }

    public static String[] NO_PARAMS = new String[0];

    public static String[] getCommandParms(Message msg) {

        if (!msg.text().contains("/")) return NO_PARAMS;

        String body = StrUtil.subAfter(msg.text(), "/", false);

        if (body.contains(" ")) {

            return StrUtil.subAfter(body, " ", false).split(" ");

        } else if (body.contains("@")) {

            return StrUtil.subAfter(body, "@", false).split(" ");

        } else {

            return NO_PARAMS;

        }

    }
    
    public CData cdata(String point) {
        
        CData data = new CData();
        
        data.setPoint(point);
        
        return data;

    }
    
    public CData cdata(String point,String index) {
        
        CData data = cdata(point);

        data.setindex(index);
        
        return data;
        
    }
    
    public CData cdata(String point,UserData userData,TwiAccount account) {

        CData data = cdata(point);

        data.setUser(userData,account);

        return data;

    }
    
    public CData cdata(String point,String index,UserData userData,TwiAccount account) {
        
        CData data = cdata(point, index);
        
        data.setUser(userData,account);
        
        return data;

    }
    
    public void delete(Message msg) {

        bot.execute(new DeleteMessage(msg.chat().id(), msg.messageId()));

    }

    public LinkedList<InlineKeyboardButton[]> newButtonList() {

        return  new  LinkedList<InlineKeyboardButton[]>();

    }

    public InlineKeyboardMarkup markup(LinkedList<InlineKeyboardButton[]> buttons) {

        return new InlineKeyboardMarkup(buttons.toArray(new InlineKeyboardButton[buttons.size()][]));

    }



}
