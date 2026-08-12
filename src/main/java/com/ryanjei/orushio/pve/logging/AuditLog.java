package com.ryanjei.orushio.pve.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AuditLog implements AuditSink {
    private final Path directory;
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    public AuditLog(Path directory) { this.directory = directory; }
    public synchronized void record(String traceId, String category, String code, String operation) {
        String line = "{\"at\":\""+escape(OffsetDateTime.now().toString())+"\",\"category\":\""+escape(category)+"\",\"code\":\""+escape(code)+"\",\"operation\":\""+escape(operation)+"\",\"traceId\":\""+escape(traceId)+"\"}\n";
        try { Files.createDirectories(directory); Files.writeString(directory.resolve("audit-"+LocalDate.now()+".jsonl"),line,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND); healthy.set(true); }
        catch (IOException e) { healthy.set(false); }
    }
    public boolean healthy(){return healthy.get();}
    static String escape(String value){StringBuilder b=new StringBuilder();for(char c:value.toCharArray()){switch(c){case '\\'->b.append("\\\\");case '"'->b.append("\\\"");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<0x20)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}}return b.toString();}
}
