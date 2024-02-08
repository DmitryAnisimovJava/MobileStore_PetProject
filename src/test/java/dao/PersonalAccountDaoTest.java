package dao;

import com.querydsl.core.Tuple;
import entity.ItemsEntity;
import entity.PersonalAccountEntity;
import entity.PremiumUserEntity;
import entity.SellHistoryEntity;
import entity.enums.CountryEnum;
import entity.enums.DiscountEnum;
import entity.enums.GenderEnum;
import extentions.PersonalAccountParameterResolver;
import jakarta.persistence.EntityExistsException;
import lombok.Cleanup;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import util.EntityHandler;
import util.HibernateTestUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;

@TestInstance(PER_METHOD)
@Tag(value = "PersonalAccountDao")
@ExtendWith(value = {PersonalAccountParameterResolver.class})
public class PersonalAccountDaoTest {
    private final PersonalAccountDao personalAccountDao;
    private static List<ItemsEntity> itemsEntities;
    private static List<SellHistoryEntity> sellHistoryEntities;
    private static List<PersonalAccountEntity> personalAccountEntities;
    private static final SessionFactory entityManager = HibernateTestUtil.getSessionFactory();

    public PersonalAccountDaoTest(PersonalAccountDao instance) {
        this.personalAccountDao = instance;
    }

    @BeforeAll
    public static void fillTableWithEntities() {
        itemsEntities = EntityHandler.getItemsEntities();
        personalAccountEntities = EntityHandler.getPersonalAccountEntities();
        var basicEntities = EntityHandler.getSellHistoryEntities();
        @Cleanup var session = entityManager.openSession();
        EntityHandler.persistEntitiesList(itemsEntities, session);
        EntityHandler.persistEntitiesList(personalAccountEntities, session);
        sellHistoryEntities = EntityHandler.createSellHistoryEntitiesList(personalAccountEntities, itemsEntities,
                                                                          basicEntities, 14);
        EntityHandler.persistEntitiesList(sellHistoryEntities, session);

    }

    @AfterAll
    public static void closeTestSessionFactory() {
        @Cleanup var session = entityManager.openSession();
        EntityHandler.dropEntities(session, sellHistoryEntities, personalAccountEntities, itemsEntities);
        entityManager.close();

    }

    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void insert_NewUser_notNull(PersonalAccountEntity account) {
        @Cleanup var session = entityManager.openSession();
        var insertResult = personalAccountDao.insert(account, session);
        assertThat(insertResult).isNotEmpty();
        EntityHandler.dropEntity(account, session);
    }

    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void insert_ExistingUser_throwException(PersonalAccountEntity account) {
        try (var session = entityManager.openSession()) {
            personalAccountDao.insert(account, session);
            session.evict(account);
            assertThatThrownBy(() -> personalAccountDao.insert(account, session)).isInstanceOf(EntityExistsException.class);
        } finally {
            @Cleanup var session = entityManager.openSession();
            EntityHandler.dropEntity(account, session);
        }
    }

    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void delete_ExistingUser_returnTrue(PersonalAccountEntity account) {
        @Cleanup var session = entityManager.openSession();
        EntityHandler.persistEntity(account, session);
        var deleteResult = personalAccountDao.delete(account, session);
        assertThat(deleteResult).isTrue();
    }

    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void delete_NotExistingUser_returnFalse(PersonalAccountEntity account) {
        @Cleanup var session = entityManager.openSession();
        personalAccountDao.delete(account, session);

    }

    @Tag("Unit")
    @Test
    void getAll_haveUsers_returnAll() {
        @Cleanup var session = entityManager.openSession();
        var resultList = personalAccountDao.getAll(session);
        assertThat(resultList).hasSameElementsAs(personalAccountEntities);
    }

    @Tag("Unit")
    @Test
    void getAllWithPhonePurchases_haveUsers_returnAll() {
        @Cleanup var session = entityManager.openSession();
        var resultList = personalAccountDao.getAllWithPhonePurchases(session);
        assertThat(resultList).hasSameElementsAs(personalAccountEntities);
    }


    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void validateAuth_validUser_returnUser(PersonalAccountEntity account) {
        @Cleanup Session session = entityManager.openSession();
        EntityHandler.persistEntity(account, session);
        Optional<PersonalAccountEntity> personalAccountEntity =
                personalAccountDao.validateAuth(account.getEmail(), account.getPassword(), session);
        assertThat(personalAccountEntity.get()).isNotNull();
        assertThat(personalAccountEntity.get().getEmail()).isEqualTo(account.getEmail());
        assertThat(personalAccountEntity.get().getPassword()).isEqualTo(account.getPassword());
        EntityHandler.dropEntity(account, session);
    }

    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void getByEmail_validUser_returnUser(PersonalAccountEntity account) {
        @Cleanup Session session = entityManager.openSession();
        EntityHandler.persistEntity(account, session);
        Optional<PersonalAccountEntity> personalAccountEntity = personalAccountDao.getByEmail(account.getEmail(),
                                                                                              session);
        assertThat(personalAccountEntity.get()).isNotNull();
        assertThat(personalAccountEntity.get().getId()).isEqualTo(account.getId());
        assertThat(personalAccountEntity.get().getEmail()).isEqualTo(account.getEmail());
        assertThat(personalAccountEntity.get().getPassword()).isEqualTo(account.getPassword());
        EntityHandler.dropEntity(account, session);
    }

    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void checkDiscount_premiumUser_returnDiscount(PersonalAccountEntity account) {
        @Cleanup Session session = entityManager.openSession();
        PremiumUserEntity premiumUserEntity = new PremiumUserEntity(account, DiscountEnum.FIVE_PERCENT);
        EntityHandler.persistEntity(premiumUserEntity, session);
        Optional<DiscountEnum> discount = personalAccountDao.checkDiscount(premiumUserEntity.getId(), session);
        assertThat(discount.get()).isEqualTo(DiscountEnum.FIVE_PERCENT);
        EntityHandler.dropEntity(premiumUserEntity, session);
    }

    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void checkDiscount_notPremiumUser_returnNull(PersonalAccountEntity account) {
        @Cleanup Session session = entityManager.openSession();
        EntityHandler.persistEntity(account, session);
        Optional<DiscountEnum> discount = personalAccountDao.checkDiscount(account.getId(), session);
        assertThat(discount).isEmpty();
        EntityHandler.dropEntity(account, session);
    }


    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void getAllBoughtPhones_havePhones_returnList(PersonalAccountEntity account) {
        List<SellHistoryEntity> sells = EntityHandler.getSellHistoryEntities().subList(0, 3);
        List<ItemsEntity> items = EntityHandler.getItemsEntities().subList(0, 3);
        @Cleanup Session session = entityManager.openSession();
        EntityHandler.persistEntity(account, session);
        EntityHandler.persistEntitiesList(items, session);
        for (int i = 0; i < sells.size(); i++) {
            SellHistoryEntity sellHistoryEntity = sells.get(i);
            sellHistoryEntity.setUser(account);
            sellHistoryEntity.setItemId(items.get(i));
        }
        EntityHandler.persistEntitiesList(sells, session);
        List<ItemsEntity> allBoughtPhones = personalAccountDao.getAllBoughtPhones(account.getId(), session);
        assertThat(allBoughtPhones.size()).isEqualTo(items.size());
        assertThat(allBoughtPhones).extracting("id")
                .contains(items.get(0).getId(), items.get(1).getId(), items.get(2).getId());
        EntityHandler.dropEntities(session, sells, items);
        EntityHandler.dropEntity(account, session);
    }

    @Tag("Unit")
    @ParameterizedTest
    @MethodSource("dao.PersonalAccountDaoTest#argumentsPersonalAccount")
    void getAllBoughtPhones_noPhones_returnEmptyList(PersonalAccountEntity account) {
        @Cleanup Session session = entityManager.openSession();
        EntityHandler.persistEntity(account, session);
        List<ItemsEntity> allBoughtPhones = personalAccountDao.getAllBoughtPhones(account.getId(), session);
        assertThat(allBoughtPhones).isEmpty();
        EntityHandler.dropEntity(account, session);
    }

    @Test
    void getTopTenMostSpenders_haveUsers_returnTop() {
        @Cleanup Session session = entityManager.openSession();
        List<Tuple> topTenMostSpenders = personalAccountDao.getTopTenMostSpenders(session);
        for (int i = 1; i < topTenMostSpenders.size(); i++) {
            Double spender = (Double) topTenMostSpenders.get(i - 1).get(1, Double.class);
            Double nextSpender = (Double) topTenMostSpenders.get(i).get(1, Double.class);
            assertThat(spender).isGreaterThan(nextSpender);
        }
    }

    @Test
    void sortByGenderAndCountry_haveUsers_returnFilteredUsers() {
        EntityHandler.getProfileInfoEntities();
    }

    public static Stream<PersonalAccountEntity> argumentsPersonalAccount() {
        return Stream.of(PersonalAccountEntity.builder().image("").name("Sergei").surname("Elanov")
                                 .email("giga@mail.ru").birthday(LocalDate.of(1990, 12, 12)).city("Moscow")
                                 .address("Pushkina").countryEnum(CountryEnum.RUSSIA)
                                 .genderEnum(GenderEnum.MALE).phoneNumber("+79553330987").password("1499")
                                 .build(),
                         PersonalAccountEntity.builder().image("")
                                 .name("Alan").surname("Kulkaev")
                                 .email("DADA009@mail.ru")
                                 .birthday(LocalDate.of(2000, 3, 10))
                                 .city("Astana").address("Lenina, b. 18")
                                 .countryEnum(CountryEnum.KAZAKHSTAN)
                                 .genderEnum(GenderEnum.MALE)
                                 .phoneNumber("+79553330987")
                                 .password("FNIM912KND")
                                 .build(),
                         PersonalAccountEntity.builder()
                                 .image("")
                                 .name("Elena")
                                 .surname("Mishkina")
                                 .email("lena_mshk@mail.ru")
                                 .birthday(LocalDate.of(1980, 6, 12))
                                 .city("Minsk")
                                 .address("Pushkina")
                                 .countryEnum(CountryEnum.BELARUS)
                                 .genderEnum(GenderEnum.MALE)
                                 .phoneNumber("+79553330987")
                                 .password("lenoff")
                                 .build());

    }
}
