package one.coffee.commands;

import chat.tamtam.bot.builders.NewMessageBodyBuilder;
import chat.tamtam.botapi.model.Message;
import one.coffee.sql.entities.UserConnection;
import one.coffee.sql.entities.UserState;
import one.coffee.sql.user.User;
import one.coffee.sql.user.UsersTable;
import one.coffee.utils.CommandHandler;
import one.coffee.utils.StaticContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.List;

public class DefaultCommandHandler extends CommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public DefaultCommandHandler() {
        super(StaticContext.getMessageSender());
    }

    @Override
    protected void handleCommand(Message message, String[] commandWithArgs) {
        switch (commandWithArgs[0]) {
            case ("/help") -> {
                handleHelp(message);
            }
            case ("/start") -> {
                handleStart(message);
            }
            default -> {
                handleDefault(message);
            }
        }
    }

    private void handleHelp(Message message) {
        messageSender.sendMessage(message.getSender().getUserId(), NewMessageBodyBuilder.ofText("""
                Список команд бота, доступных для использования:
                /help - список всех команд
                /start - начать диалог с пользователем""").build());
    }

    private void handleStart(Message message) {
        try {
            long senderId = message.getSender().getUserId();
            User sender = UsersTable.getUserByUserId(senderId);
            if (sender == null) {
                // См. возможные причины в OneCoffeeUpdateHandler::visit(MessageCreatedUpdate)
                sender = new User(senderId, "Cyberpunk2077", UserState.DEFAULT);
                sender.commit();
            }

            List<User> userWaitList = UsersTable.getWaitingUsers(1);
            if (userWaitList.isEmpty()) {
                startTheWait(message);
                return;
            }

            User recipient = userWaitList.get(0);
            UserConnection userConnection = new UserConnection(senderId, recipient.getId());
            userConnection.commit();

            messageSender.sendMessage(senderId,
                    NewMessageBodyBuilder.ofText("""
                            Я нашел вам собеседника!
                            Я буду передавать сообщения между вами, можете общаться сколько влезет!)
                            Список команд, доступных во время беседы можно открыть на /help\s""").build());
            messageSender.sendMessage(recipient.getId(),
                    NewMessageBodyBuilder.ofText("""
                            Я нашел вам собеседника!
                            Я буду передавать сообщения между вами, можете общаться сколько влезет!)
                            Список команд, доступных во время беседы можно открыть на /help\s""").build());
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        }
    }

    private void startTheWait(Message message) {
        long senderId = message.getSender().getUserId();

        try {
            User sender = UsersTable.getUserByUserId(senderId);
            if (sender == null) {
                // См. возможные причины в OneCoffeeUpdateHandler::visit(MessageCreatedUpdate)
                sender = new User(senderId, "Cyberpunk2077", UserState.DEFAULT);
            }
            sender.setState(UserState.WAITING);
            sender.commit();
            messageSender.sendMessage(message.getSender().getUserId(),
                    NewMessageBodyBuilder.ofText("Вы успешно добавлены в список ждущий пользователей! Ожидайте начала диалога! ").build());
        } catch (SQLException e) {
            LOG.warn("Putting user " + senderId + " in waiting list was interrupted");
            messageSender.sendMessage(senderId,
                    NewMessageBodyBuilder.ofText("Встать в список ждущих пользователей не получилось, попробуй чуть позже :( ").build());
        }
    }

}
