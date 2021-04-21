package org.forfun.mmorpg.net.message;

import org.forfun.mmorpg.common.util.ClassScanner;
import org.forfun.mmorpg.net.message.codec.impl.reflect.Codec;
import org.forfun.mmorpg.net.socket.annotation.MessageMeta;
import org.forfun.mmorpg.net.socket.annotation.ModuleMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class MessageFactory {

    private static MessageFactory instance = new MessageFactory();

    private Map<Short, Class<? extends Message>> id2Clazz = new HashMap<>();

    private Map<Class<? extends Message>, Short> clazz2Id = new HashMap<>();

    public static MessageFactory getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public void init(String path) {
        Set<Class<?>> messages = ClassScanner.listClassesWithAnnotation(path, ModuleMeta.class);
        for (Class<?> clazz : messages) {
            ModuleMeta meta = clazz.getAnnotation(ModuleMeta.class);
            if (meta == null) {
                throw new RuntimeException("messages[" + clazz.getSimpleName() + "] missed MessageMeta annotation");
            }
            short module = meta.module();
            if (Math.abs(module) > 326) {
                throw new RuntimeException("abs(module) must less than 326, target " + module);
            }

            // facade层所在包名的上一层
            int prevPacketNameIndex = clazz.getPackage().getName().lastIndexOf(".");
            String packetName = clazz.getPackage().getName().substring(0, prevPacketNameIndex);
            Set<Class<?>> msgClazzs = ClassScanner.listAllSubclasses(packetName, Message.class);
            msgClazzs.parallelStream()
                    .filter(msgClz -> msgClz.getAnnotation(MessageMeta.class) != null)
                    .forEach(msgClz -> {
                        MessageMeta mapperAnnotation = msgClz.getAnnotation(MessageMeta.class);
                        byte cmdMeta = mapperAnnotation.cmd();
                        if (Math.abs(cmdMeta) > 99) {
                            throw new RuntimeException("abs(cmd) must less than 100, target " + msgClz.getSimpleName());
                        }
                        short key = (short) (Math.abs(module) * 100 + cmdMeta);
                        if (module < 0) {
                            key = (short) (0 - key);
                        }
                        if (id2Clazz.containsKey(key)) {
                            throw new RuntimeException("message meta [" + key + "] duplicate！！");
                        }
                        clazz2Id.put((Class<? extends Message>) msgClz, key);
                        id2Clazz.put(key, (Class<? extends Message>) msgClz);
                        Codec.registerClass(msgClz, key);
                    });
        }

    }

    /**
     * 返回消息的模板class
     *
     * @param cmd
     * @return
     */
    public Class<? extends Message> getMessageMeta(short cmd) {
        return id2Clazz.get(cmd);
    }

    public short getMessageId(Class<?> clazz) {
        if (clazz == null || !clazz2Id.containsKey(clazz)) {
            throw new IllegalPacketException(clazz.getSimpleName() + "未注册");
        }
        return clazz2Id.get(clazz);
    }

    public Set<Class<? extends Message>> listAllMessages() {
        return new HashSet<>(clazz2Id.keySet());
    }

}
