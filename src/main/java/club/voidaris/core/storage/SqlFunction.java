package club.voidaris.core.storage;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<T> {

    T apply(Connection connection) throws SQLException;
}
