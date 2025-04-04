package cn.travellerr.onebottelegram.converter;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.travellerr.onebotApi.*;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.hibernate.HibernateUtil;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.onebotWebsocket.OneBotWebSocketHandler;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetChatMemberCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TelegramToOnebot implements ApplicationRunner {

    public static final Map<Integer, Long> messageIdToChatId = new java.util.LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Long> eldest) {
            return size() > 512;
        }
    };


    private static final Logger log = LoggerFactory.getLogger(TelegramToOnebot.class);

    public static void forwardToOnebot(Update update) {
        if (update.message() != null&&update.message().text() != null) {

            messageIdToChatId.put(update.message().messageId(), Math.abs(update.message().chat().id()));

            // 截取"@"前消息
            String realMessage = update.message().text().replace("@"+TelegramApi.getMeResponse.user().username(), "").trim();

            String username = update.message().from().username();
            if (username == null) {
                username = update.message().from().firstName();
            }

            JSONObject object;

            realMessage = serializeCommand(realMessage);


            JSONArray message = new JSONArray().set(new Text(realMessage));


            if (update.message().chat().type().equals(Chat.Type.group) || update.message().chat().type().equals(Chat.Type.supergroup)) {
                Group group = null;

                try {
                    group = HibernateFactory.selectOne(Group.class, update.message().chat().id());
                } catch (Exception ignored) {
                }

                if (group == null) {
                    int memberCount = TelegramApi.bot.execute(new GetChatMemberCount(update.message().chat().id())).count();
                    group = Group.builder()
                            .groupId(update.message().chat().id())
                            .groupName(update.message().chat().title())
                            .memberCount(memberCount)
                            .build();
                    HibernateFactory.merge(group);
                }

                group.addMemberId(update.message().from().id());
                group.addMemberUsernames(username);
                HibernateFactory.merge(group);

                if (realMessage.contains("@")) {
                    List<Long> atList = new ArrayList<>();
                    List<String> messageList = new ArrayList<>();
                    Matcher matcher = Pattern.compile("@(\\S+?)(\\s|$)").matcher(realMessage);

                    List<Long> membersIdList = group.getMembersIdList();
                    List<String> memberUsernameList = group.getMemberUsernamesList();


                    int lastIndex = 0;
                    while (matcher.find()) {
                        int index = memberUsernameList.indexOf(matcher.group(1));
                        if (index != -1) {
                            atList.add(membersIdList.get(index));
                            String msg = realMessage.substring(lastIndex, matcher.start()).trim();
                            if (!messageList.isEmpty()) {
                                msg = " " + msg;
                            }
                            messageList.add(msg);
                            lastIndex = matcher.end();
                        }
                        messageList.add(" "+realMessage.substring(lastIndex).trim());
                    }

                    message = new JSONArray();
                    for (int i = 0; i < messageList.size(); i++) {
                        Text messageObject = new Text(messageList.get(i));
                        if (!messageObject.getData().getText().isEmpty()) {
                            message.add(messageObject);
                        }
                        if (i < atList.size()) {
                            At atObject = new At(atList.get(i));
                            message.add(atObject);
                        }


                    }


                }

                Sender groupSender = new Sender(update.message().from().id(), username, update.message().from().firstName(), "unknown", 0, "虚拟地区", "0", "member", "");
                GroupMessage groupMessage = new GroupMessage(System.currentTimeMillis(), TelegramApi.getMeResponse.user().id(), "message", "group", "normal", update.message().messageId(), -update.message().chat().id(), update.message().from().id(), null, realMessage, 0, groupSender);

                object = new JSONObject(groupMessage);
            } else {
                Sender sender = new Sender(update.message().from().id(), username, update.message().from().firstName(), "unknown", 0, null, null, null, null);
                PrivateMessage privateMessage = new PrivateMessage(System.currentTimeMillis(), TelegramApi.getMeResponse.user().id(), "message", "private", "friend", update.message().messageId(), update.message().from().id(), realMessage, 0, sender);
                object = new JSONObject(privateMessage);
            }

            if (!TelegramOnebotAdapter.config.getOnebot().isUseArray()) {
                object.set("message", arrayMessageToString(message));
            } else {
                object.set("message", message);
            }

            log.info("发送消息至 Onebot --> {}", object);

            OneBotWebSocketHandler.broadcast(object.toString());


        }
    }

    private static String serializeCommand(String realMessage) {
        String tempStr = String.copyValueOf(realMessage.toCharArray());
        Map<String, String> commandMap = TelegramOnebotAdapter.config.getCommand().getCommandMap();
        String prefix = TelegramOnebotAdapter.config.getCommand().getPrefix();
        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            String key = prefix+entry.getKey();
            String value = prefix+entry.getValue();
            tempStr = tempStr.replace(key, value);
        }

        return tempStr;
    }

    public static String arrayMessageToString(JSONArray arrayMessage) {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < arrayMessage.size(); i++) {
            JSONObject messageObject = arrayMessage.getJSONObject(i);
            if (messageObject.getStr("type").equals("text")) {
                message.append(specialCharacterEscape(messageObject.getJSONObject("data").getStr("text")));
            } else if (messageObject.getStr("type").equals("at")) {
                message.append("[CQ:at,qq=").append(specialCharacterEscape(messageObject.getJSONObject("data").getStr("qq"))).append("]");
            } else if(messageObject.getStr("type").equals("image")) {
                message.append("[CQ:image,file=").append(specialCharacterEscape(messageObject.getJSONObject("data").getStr("file"))).append("]");
            }
        }
        return message.toString();
    }

    public static String stringMessageToArray(String message) {
        JSONArray arrayMessage = new JSONArray();
        Matcher matcher = Pattern.compile("\\[CQ:(\\S+?)(,\\S+)?]").matcher(message);
        int lastIndex = 0;
        while (matcher.find()) {
            String msg = message.substring(lastIndex, matcher.start());
            if (!msg.isEmpty()) {
                Text messageObject = new Text(specialCharacterUnescape(msg));
                arrayMessage.add(messageObject);
            }
            lastIndex = matcher.end();
            String type = matcher.group(1);
            String data = matcher.group(2);
            if (type.equals("at")) {
                At atObject = new At(Long.parseLong(data.substring(4)));
                arrayMessage.add(atObject);
            } else if (type.equals("image")) {
                Image imageObject = new Image(data.substring(5));
                arrayMessage.add(imageObject);
            }
        }
        String msg = message.substring(lastIndex);
        if (!msg.isEmpty()) {
            Text messageObject = new Text(specialCharacterUnescape(msg));
            arrayMessage.add(messageObject);
        }
        return arrayMessage.toString();
    }

    private static String specialCharacterEscape(String message) {
        return message.replace("&", "&amp;")
                .replace("[", "&#91;")
                .replace("]", "&#93;")
                .replace(",", "&#44;");
    }

    private static String specialCharacterUnescape(String message) {
        return message.replace("&#44;", ",")
                .replace("&#93;", "]")
                .replace("&#91;", "[")
                .replace("&amp;", "&");
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Telegram to Onebot converter is running...");
        HibernateUtil.init(TelegramOnebotAdapter.INSTANCE);
        TelegramApi.init();
    }
}
