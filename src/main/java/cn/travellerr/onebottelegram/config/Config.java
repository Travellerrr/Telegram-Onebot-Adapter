package cn.travellerr.onebottelegram.config;

import cn.chahuyun.hibernateplus.DriveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ConfigEntity(name = "config", filePath = "./")
public class Config implements Serializable {

    private telegram telegram;
    private onebot onebot;
    private spring spring;
    private command command;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class telegram implements Serializable {

        private Config.telegram.bot bot;
        private Config.telegram.webhook webhook;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class bot implements Serializable {
            private String token;
            private String username;
            private Config.telegram.bot.proxy proxy;
            private boolean useTranslator;

            @Data
            @Builder
            @AllArgsConstructor
            @NoArgsConstructor
            public static class proxy implements Serializable {
                private String host;
                private int port;
                private String username;
                private String secret;
                private String type;
            }


        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class webhook implements Serializable {
            private String certPath;
            private String secret;
            private String url;
            private int port;
            private boolean useWebhook;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class onebot implements Serializable {
            private String ip;
            private String path;
            private int port;
            private String token;
            private boolean useArray;
            private boolean banGroupUser;
            private String groupUserWarning;
            private boolean picBase64;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class spring implements Serializable {
        private Config.spring.jackson jackson;
        private Config.spring.database database;
        private Config.spring.webui webui;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class jackson implements Serializable {
            private String dateformat;
            private String timezone;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class database implements Serializable {
            private DriveType dataType;
            private String mysqlUrl;
            private String mysqlUser;
            private String mysqlPassword;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class webui implements Serializable {
            private String userName;
            private String password;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class command implements Serializable {
        private String prefix;
        private Map<String, String> menu;
        private Map<String, String> commandMap;
    }
}
