package server;

import java.io.IOException;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ldr.client.domen.db.DataBase;
import ldr.client.domen.db.IDataBase;

@Configuration
public class DataBaseProvider {
    @Bean
    public IDataBase dataBase(@Value("${database.location}") String databaseLocation) throws IOException {
        return DataBase.load(Paths.get(databaseLocation));
    }
}
