package unit;

import dao.ItemsDao;
import dao.PersonalAccountDao;
import dao.SellHistoryDao;
import entity.*;
import extentions.PersonalAccountParameterResolver;
import extentions.SellHistoryParameterResolver;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import utlis.HibernateSessionFactory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
public class DaoTest {
    @Nested
    @Tag(value = "ItemsDao")
    class Items {
        ItemsDao itemsDao = ItemsDao.getInstance();
        ItemsEntity itemsEntity = ItemsEntity.builder().model("12").brand(BrandEnum.OnePlus)
                .attributes("512 gb white").price(99_999.99)
                .currency(CurrencyEnum.₽).quantity(233).build();

        @Test
        void insertMethodReturnsUserIdFromDB() {
            itemsDao.insert(itemsEntity);
            log.info("Just added: {} {} {} {} qt: {}", itemsEntity.getBrand(), itemsEntity.getModel(),
                     itemsEntity.getPrice(), itemsEntity.getCurrency(), itemsEntity.getQuantity());
        }

        @Test
        void findAllMethodReturnList() {
            List<ItemsEntity> all = itemsDao.findAll();
            assertThat(all).hasSize(15);
        }
    }


    @Nested
    @TestInstance(value = Lifecycle.PER_CLASS)
    @Tag(value = "PersonalAccountDao")
    @ExtendWith(value = {PersonalAccountParameterResolver.class})
    class PersonalAccount {

        private PersonalAccountDao instance;

        public PersonalAccount(PersonalAccountDao instance) {
            this.instance = instance;
        }

        @Tag("Unit")
        @ParameterizedTest
        @MethodSource("getArgumentForPersonalAccountTest")
        void insert_NewUser_notNull(PersonalAccountEntity account) {
            @Cleanup SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactory();
            @Cleanup Session session = sessionFactory.openSession();
            sessionFactory.close();

            Optional<Long> insert = instance.insert(account);
            assertThat(insert).isNotEmpty();
        }

        @Tag("Unit")
        @ParameterizedTest
        @MethodSource("getArgumentForPersonalAccountTest")
        void insertMethodAddUserReturnsUserIdFromDB(PersonalAccountEntity account) {
            Optional<Long> insert = instance.insert(account);
            assertThat(insert).isNotEmpty();
        }

        static Stream<Arguments> getArgumentForPersonalAccountTest() {
            return Stream.of(Arguments.of(PersonalAccountEntity.builder().address("no address")
                                                  .birthday(LocalDate.now().minusYears(20)).city("no city")
                                                  .country(Country.KAZAKHSTAN)
                                                  .email("noemail@email.ru").gender(Gender.MALE).image("").name("Sasha")
                                                  .password("123")
                                                  .phoneNumber("+79214050505").surname("nonamich").build()));
        }

    }

    @Nested
    @TestInstance(value = Lifecycle.PER_METHOD)
    @Tag(value = "PersonalAccountDao")
    @ExtendWith({SellHistoryParameterResolver.class})
    class SellHistoryTest {

        private SellHistoryDao instance;

        public SellHistoryTest(SellHistoryDao instance) {
            this.instance = instance;
        }

        @Tag("Unit")
        @ParameterizedTest
        @MethodSource("getArgumentForSellHistory")
        void insert_NewSell_notNull(SellHistoryEntity entity) {
            Optional<Long> id = instance.insert(entity);
            assertThat(id).isNotEmpty();
            @Cleanup SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactory();
            @Cleanup Session session = sessionFactory.openSession();
            Transaction transaction = session.beginTransaction();
            id.ifPresent(sellId -> {
                SellHistoryEntity sellHistoryEntity = session.get(SellHistoryEntity.class, sellId);
                session.remove(sellHistoryEntity);
                transaction.commit();
            });
        }


        public static Stream<Arguments> getArgumentForSellHistory() {
            return Stream.of(Arguments.of(SellHistoryEntity.builder().sellDate(OffsetDateTime.now())
                                                  .user(PersonalAccountEntity.builder().accountId(30L).build())
                                                  .itemId(1L).quantity(2).build()));
        }
    }
}
