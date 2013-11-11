package io.pillopl.card;

import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The database package RFID_UTILS converts UIDs exactly like {@link RfidConverter}.
 */
class RfidUtilsIntegrationTest extends IntegrationTest {

    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void databaseAndJavaConvertTheSameWay() {
        for (String hex : List.of("04A1B2C3", "04a1b2c3d4e5f6", "ABC", "00", "FFFFFFFFFFFFFFFF")) {
            String decimal = jdbcTemplate.queryForObject("SELECT RFID_UTILS.HEX_BIGENDIAN_TO_DEC(?)::TEXT",
                    String.class, hex);
            assertThat(decimal).as(hex).isEqualTo(RfidConverter.hexBigEndianToDecimal(hex));

            String back = jdbcTemplate.queryForObject("SELECT RFID_UTILS.DEC_TO_HEX_BIGENDIAN(?::TEXT)",
                    String.class, decimal);
            assertThat(back).as(decimal).isEqualTo(RfidConverter.decimalToHexBigEndian(decimal));
        }
    }

    @Test
    void plainHexConversionAndByteOrder() {
        assertThat(jdbcTemplate.queryForObject("SELECT RFID_UTILS.HEX_TO_DEC('ff')::TEXT", String.class))
                .isEqualTo("255");
        assertThat(jdbcTemplate.queryForObject("SELECT RFID_UTILS.DEC_TO_HEX(0)", String.class)).isEqualTo("0");
        assertThat(jdbcTemplate.queryForObject("SELECT RFID_UTILS.TURN_BYTE_ORDER('abc')", String.class))
                .isEqualTo(RfidConverter.turnByteOrder("abc"));
    }

    @Test
    void notAHexDigitIsAnError() {
        assertThatThrownBy(() -> jdbcTemplate.queryForObject("SELECT RFID_UTILS.HEX_TO_DEC('04G1')", String.class))
                .isInstanceOf(UncategorizedSQLException.class);
    }
}
