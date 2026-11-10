package bo.cdr.app.util;

import bo.cdr.app.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortUtilTest {

    private static final Set<String> ALLOWED = Set.of("id", "name");

    @Test
    void build_returnsRequestedOrder_whenFieldAllowed() {
        Sort sort = SortUtil.build("name", "asc", ALLOWED);
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void build_defaultsToIdDesc_whenNoFieldOrDirection() {
        Sort sort = SortUtil.build(null, null, ALLOWED);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void build_throws_whenFieldNotInWhitelist() {
        assertThatThrownBy(() -> SortUtil.build("password", "asc", ALLOWED))
                .isInstanceOf(InvalidValueException.class);
    }
}
