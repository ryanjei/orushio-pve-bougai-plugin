package com.ryanjei.orushio.pve.bootstrap;

import java.nio.file.*;

public final class LauncherShutdownHandoff {
    private final Path target;
    public LauncherShutdownHandoff(Path target){this.target=target.toAbsolutePath().normalize();}
    public void clear(){try{Files.deleteIfExists(target);Files.deleteIfExists(temp());}catch(Exception exception){throw new IllegalStateException("古い安全停止情報を削除できません。",exception);}}
    public void publish(String token){Path temporary=temp();try{Files.createDirectories(target.getParent());Files.writeString(temporary,token,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}catch(Exception exception){try{Files.deleteIfExists(temporary);}catch(Exception ignored){}throw new IllegalStateException("安全停止の準備ができません。",exception);}}
    private Path temp(){return target.resolveSibling(target.getFileName()+".tmp");}
}
