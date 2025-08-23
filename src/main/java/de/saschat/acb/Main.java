package de.saschat.acb;

import com.eduardomcb.discord.webhook.WebhookClient;
import com.eduardomcb.discord.webhook.WebhookManager;
import com.eduardomcb.discord.webhook.models.Message;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.WebhookAction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main implements EventListener {
    public static void main(String[] args) throws Throwable {
        String path = null;
        if (args.length > 0)
            path = args[0];
        new Main().main(path);
    }

    Process server;
    PrintWriter writer;
    Config cfg;

    public void main(String path) throws InterruptedException, IOException {
        cfg = Config.load(path);
        if (cfg.cmdline == null) {
            System.out.println("Server process commandline is invalid.");
            return;
        }
        if (cfg.token == null) {
            System.out.println("Token is invalid.");
            return;
        }
        if (cfg.channel == null) {
            System.out.println("Channel is invalid.");
            return;
        }

        JDA jda = JDABuilder.createDefault(cfg.token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build();
        jda.awaitReady();

        TextChannel channel = jda.getChannelById(TextChannel.class, cfg.channel);
        if (channel == null) {
            System.out.println("Channel not found.");
            return;
        }


        WebhookManager webhookManager = null;
        if (cfg.webhook != null) {

            String url = resolveWebhook(cfg.webhook);

            if (url == null) {
                System.out.println("Webhook invalid.");
            } else {
                webhookManager = new WebhookManager().setChannelUrl(url);
                cfg.webhook = url;
                cfg.dirty();
            }
        } else {
            System.out.println("Webhook is not set.");
        }
        if (webhookManager == null) {
            WebhookAction acb = channel.createWebhook("acb");
            Webhook hook = acb.complete();
            if (hook != null) {
                cfg.webhook = resolveWebhook(hook.getUrl());
                cfg.dirty();
                if (cfg.webhook != null)
                    webhookManager = new WebhookManager().setChannelUrl(cfg.webhook);
            }
        }
        if (webhookManager == null) {
            System.out.println("Unable to find or create webhook.");
            return;
        }
        webhookManager.setListener(new WebhookClient.Callback() {
            @Override
            public void onSuccess(String response) {
                // do nothing
            }

            @Override
            public void onFailure(int statusCode, String errorMessage) {
                System.err.println("Webhook failed with status code " + statusCode + ": " + errorMessage);
            }
        });

        Pattern start = Pattern.compile(cfg.startPattern);
        Pattern message = Pattern.compile(cfg.messagePattern);
        Pattern join = Pattern.compile(cfg.joinedPattern);
        Pattern leave = Pattern.compile(cfg.leavePattern);
        Pattern die = Pattern.compile(cfg.deathPattern);
        Pattern advance = Pattern.compile(cfg.advancementPattern);
        Pattern me = Pattern.compile(cfg.mePattern);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.destroyForcibly();
        }));

        server = Runtime.getRuntime().exec(cfg.cmdline);
        writer = new PrintWriter(new OutputStreamWriter(server.getOutputStream()));


        boolean started = false;
        // io loop
        Scanner scanner = new Scanner(server.getInputStream());

        List<String> onlinePlayers = new LinkedList<>();
        jda.getPresence().setActivity(Activity.playing("with " + onlinePlayers.size() + " player(s)."));

        while (scanner.hasNextLine()) {
            try {
                String line = scanner.nextLine();

                System.out.println(line);

                Matcher meMatcher = me.matcher(line);
                if(meMatcher.find()) {
                    channel.sendMessage(meMatcher.group("text")).setAllowedMentions(List.of()).complete();
                    continue;
                }

                Matcher msg = message.matcher(line);
                if (msg.find()) {
                    String mcMsg = msg.group("msg");
                    String mcName = msg.group("name");
                    if (mcMsg != null && mcName != null) {
                        Message dcMsg = new Message();
                        dcMsg.setAvatarUrl("https://mc-heads.net/head/" + mcName);
                        dcMsg.setUsername(mcName);
                        dcMsg.setContent(mcMsg);
                        webhookManager.setMessage(dcMsg);
                        webhookManager.exec();
                    }
                    continue;
                }

                Matcher startMatch = start.matcher(line);
                if (startMatch.find() && !started) {
                    started = true;
                    channel.sendMessage(":white_check_mark: Server started!").complete();
                    continue;
                }

                Matcher joinMatcher = join.matcher(line);
                if (joinMatcher.find()) {
                    onlinePlayers.add(joinMatcher.group("name"));
                    jda.getPresence().setActivity(Activity.playing("with " + onlinePlayers.size() + " player(s)."));
                    channel.sendMessage(":inbox_tray: " + joinMatcher.group("name") + " joined!").complete();
                    continue;
                }
                Matcher leaveMatcher = leave.matcher(line);
                if (leaveMatcher.find()) {
                    onlinePlayers.remove(leaveMatcher.group("name"));
                    jda.getPresence().setActivity(Activity.playing("with " + onlinePlayers.size() + " player(s)."));
                    channel.sendMessage(":outbox_tray: " + leaveMatcher.group("name") + " left!").complete();
                    continue;
                }

                Matcher deathMatcher = die.matcher(line);
                if (deathMatcher.find() && onlinePlayers.contains(deathMatcher.group("name"))) {
                    channel.sendMessage(":skull: " + deathMatcher.group("name") + " " + deathMatcher.group("action")).complete();
                    continue;
                }
                Matcher advanceMatch = advance.matcher(line);
                if (advanceMatch.find()) {
                    channel.sendMessage(":sparkles: " + advanceMatch.group("name") + " has made the advancement **[" + advanceMatch.group("advancement") + "]**").complete();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        channel.sendMessage(":x: Server stopped!").complete();
        jda.shutdown();
    }

    private String resolveWebhook(String webhook) {
        try {
            URL url = new URL(webhook);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            String text = new String(conn.getInputStream().readAllBytes());


            int code = conn.getResponseCode();
            conn.disconnect();

            if (code == 200) {
                JsonObject jsonElement = new Gson().fromJson(text, JsonObject.class);
                return (jsonElement.get("url").getAsString());
            }

        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof ReadyEvent)
            System.out.println("API is ready!");
        if (event instanceof MessageReceivedEvent recv && !recv.isWebhookMessage() && !recv.getAuthor().isBot()) {
            if (recv.getChannel().getId().equals(cfg.channel)) {
                try {
                    String txt = cfg.receiveTemplate;
                    txt = txt.replace("%name%", recv.getAuthor().getName());
                    txt = txt.replace("%message%", recv.getMessage().getContentStripped());
                    txt = txt.replace("%id%", recv.getAuthor().getId());

                    writer.println(txt);
                    writer.flush();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
}