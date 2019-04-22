package com.tangenta.gqljs;

import lombok.val;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Scanner {
    private final String[] store;
    private int index;

    public Scanner(String src) {
        store = tokenize(src).toArray(new String[0]);
        System.out.println(Arrays.toString(store));
        index = 0;
    }

    public boolean hasNext() {
        return index < store.length;
    }

    public String next() {
        val result = peek();
        index++;
        return result;
    }

    public String peek() {
        if (index < store.length) {
            return store[index];
        } else {
            throw new NoSuchElementException();
        }
    }

    private static String SPLITTERS = ",():|={}#";

    private static List<String> tokenize(String source) {
        val tokens = new LinkedList<String>();
        val collector = new StringBuilder();
        val isInsideComment = new AtomicBoolean(false);
        source.chars().mapToObj(c -> (char) c).forEachOrdered(ch -> {
            if (ch.equals('\n')) isInsideComment.set(false);
            if (isInsideComment.get()) return;
            if (isComment(ch)) {
                isInsideComment.set(true);
                return;
            }

            if (Character.isWhitespace(ch)) {
                tryFlush(tokens, collector);
            } else if (SPLITTERS.indexOf(ch) != -1) {
                tryFlush(tokens, collector);
                tokens.add(ch.toString());
            } else {
                collector.append(ch);
            }
        });
        tryFlush(tokens, collector);
        return tokens;
    }

    private static void tryFlush(List<String> result, StringBuilder buffer) {
        if (buffer.length() != 0) {
            result.add(buffer.toString());
            buffer.delete(0, buffer.length());
        }
    }

    private static boolean isComment(Character x) {
        return x.equals('#');
    }

    public static void main(String[] args) {
        val data = "@query hots: HotsResult! # 依赖于日期，每日更新，缓存\n" +
                "\n" +
                "union HotsResult = Error | Hots\n" +
                "type Hots {\n" +
                "    hots: [HotItem!]!\n" +
                "}\n" +
                "union HotItem = SchoolHeatInfo | EntertainmentInfo | LearningResourceInfo\n";

        System.out.println(tokenize(data));
    }

}
