package org.andywang.wildpointer.config;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

import java.sql.Types;

public class SQLiteDialect extends Dialect {
    public SQLiteDialect() {
        registerColumnType(Types.BIT, "boolean");
        registerColumnType(Types.INTEGER, "integer");
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.VARCHAR, "varchar");
        // 如果报错提示找不到其他类型，可以在这里继续 register
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl() {
            @Override
            public boolean supportsIdentityColumns() { return true; }
            @Override
            public String getIdentitySelectString(String table, String column, int type) {
                return "select last_insert_rowid()";
            }
            @Override
            public String getIdentityColumnString(int type) { return "integer"; }
        };
    }

    @Override
    public boolean hasAlterTable() { return false; }

    @Override
    public boolean dropConstraints() { return false; }

    @Override
    public String getDropForeignKeyString() { return ""; }

    @Override
    public String getAddForeignKeyConstraintString(String cn, String[] fk, String t, String[] pk, boolean r) {
        return "";
    }
}