package com.nwav.editorpdf.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Slf4j
public class NwavBaseDatosConfig {
    public static final int NWAV_ESPERA_BLOQUEO_MS = 5000;

    @Bean
    public DataSource nwavDataSource(@Value("${nwav.datos.carpeta}") String nwavCarpeta,
                                     @Value("${nwav.datos.archivo}") String nwavArchivo) throws IOException {
        Path nwavDirectorio = Path.of(nwavCarpeta).toAbsolutePath().normalize();
        Files.createDirectories(nwavDirectorio);
        Path nwavRutaBase = nwavDirectorio.resolve(nwavArchivo);
        nwavLog.info("Base de datos: {}", nwavRutaBase);

        HikariConfig nwavConfig = new HikariConfig();
        nwavConfig.setPoolName("nwav-sqlite");
        nwavConfig.setDriverClassName("org.sqlite.JDBC");
        nwavConfig.setJdbcUrl("jdbc:sqlite:" + nwavRutaBase);
        nwavConfig.setMaximumPoolSize(1);
        nwavConfig.setMinimumIdle(1);
        nwavConfig.addDataSourceProperty("journal_mode", "WAL");
        nwavConfig.addDataSourceProperty("synchronous", "NORMAL");
        nwavConfig.addDataSourceProperty("foreign_keys", "true");
        nwavConfig.addDataSourceProperty("busy_timeout", String.valueOf(NWAV_ESPERA_BLOQUEO_MS));
        return new HikariDataSource(nwavConfig);
    }
}
