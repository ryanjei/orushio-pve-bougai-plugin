package com.ryanjei.orushio.pve.bootstrap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class BootstrapHandoff {
    private final Path target;
    public BootstrapHandoff(Path target){this.target=target.toAbsolutePath().normalize();}
    public void clear(){try{Files.deleteIfExists(target);Files.deleteIfExists(temp());}catch(Exception exception){throw new IllegalStateException("古い管理画面起動情報を削除できません。",exception);}}
    public void publish(int port,String token){Path temporary=temp();try{Files.createDirectories(target.getParent());String url="http://127.0.0.1:"+port+"/auth/bootstrap?token="+URLEncoder.encode(token,StandardCharsets.UTF_8);Files.writeString(temporary,url,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}catch(Exception exception){try{Files.deleteIfExists(temporary);}catch(Exception ignored){}throw new IllegalStateException("管理画面を安全に開く準備ができません。",exception);}}
    private Path temp(){return target.resolveSibling(target.getFileName()+".tmp");}
}
