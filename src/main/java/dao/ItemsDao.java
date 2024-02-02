package dao;

import com.querydsl.jpa.impl.JPAQuery;
import dto.AttributesFilter;
import entity.ItemsEntity;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import utlis.ConnectionPoolManager;
import utlis.HibernateSessionFactory;
import utlis.SqlExceptionLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static entity.QItemsEntity.itemsEntity;

@Slf4j
public class ItemsDao implements Dao<Long, ItemsEntity> {
    private static ItemsDao INSTANCE;

    private static SqlExceptionLogger SQL_EXCEPTION_LOGGER = SqlExceptionLogger.getInstance();

    public static ItemsDao getInstance() {
        if (INSTANCE == null) {
            synchronized (ItemsDao.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ItemsDao();
                    log.info("Info: Jump in synchronized block to take ItemsDao instance");
                    log.trace("Trace: Jump in synchronized block to take ItemsDao instance");
                }
            }
        }
        return INSTANCE;
    }

    private ItemsDao() {
    }

    public Optional<Long> insertViaHibernate(ItemsEntity items, Session session) {
        session.persist(items);
        return Optional.ofNullable(items.getItemId());
    }

    public List<ItemsEntity> findByBrandViaHQL(String brand, Session session) {
        return session.createQuery("select i from items i " +
                        "where i.brand = :brand", ItemsEntity.class)
                .setParameter("brand", brand)
                .list();
    }

    public List<ItemsEntity> findItemsLimitOffsetViaQuerydsl(int limit, int offset, Session session) {
        return new JPAQuery<ItemsEntity>(session).select(itemsEntity)
                .from(itemsEntity)
                .limit(limit)
                .offset(offset*3)
                .fetch();
    }

    public List<ItemsEntity> findAllViaQuerydsl(Session session) {
        return new JPAQuery<ItemsEntity>(session).select(itemsEntity)
                .from(itemsEntity)
                .fetch();
    }

    public List<ItemsEntity> findItemsUsingAttributes(AttributesFilter filter, Session session) {
        QPredicate.builder().add(filter.)
    }

    @Override
    public Optional<Long> insert(ItemsEntity entity) {
        return Optional.empty();
    }

    /*
     * Bad practice!
     * An anti-pattern is a common response to a recurring problem that is usually
     * ineffective and risks being highly counterproductive.
     */
    @Override
    public List<ItemsEntity> findAll() {
        SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactory();
        List<ItemsEntity> resultList;
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            log.info("Transaction {} for findAll just opening", transaction);
            Query<ItemsEntity> selectQuery = session.createQuery("SELECT o FROM items o", ItemsEntity.class);
            log.info("Query in persistant state: {}", selectQuery.getResultList());
            resultList = selectQuery.getResultList();
            log.info("Resulting list of entity {}", resultList);
            session.getTransaction()
                    .commit();
        }
        return resultList;
    }


    //    public Long Insert(List<ItemsEntity> arrayList) {
//        String innerSql = "(?, ?, ?, ?, ?, ?)";
//        ArrayList<String> valuesList = new ArrayList<String>();
//        for (int i = 0; i < arrayList.size(); i++) {
//            valuesList.add(innerSql);
//        }
//        String collect = valuesList.stream().collect(Collectors.joining(", ", "VALUES ", ";"));
//
////		System.out.println(valuesList);
//
//        try (Connection connection = ConnectionPoolManager.get();
//             PreparedStatement prepareStatement = connection.prepareStatement(SQL_INSERT_STATEMENT + collect,
//                     Statement.RETURN_GENERATED_KEYS)) {
////			System.out.println(SQL_INSERT_STATEMENT + collect);
//            int qt = DEFAULT_COLUMNS_QT_IN_ITEMS_TABLE; // qt - quantity
//            for (int i = 0; i < arrayList.size(); i++) {
//                prepareStatement.setString(qt * i + 1, arrayList.get(i).getModel());
//                prepareStatement.setString(qt * i + 2, arrayList.get(i).getBrand());
//                prepareStatement.setString(qt * i + 3, arrayList.get(i).getAttributes());
//                prepareStatement.setDouble(qt * i + 4, arrayList.get(i).getPrice());
//                prepareStatement.setString(qt * i + 5, arrayList.get(i).getCurrency());
//                prepareStatement.setInt(qt * i + 6, arrayList.get(i).getQuantity());
//            }
//            int executeUpdate = prepareStatement.executeUpdate();
//            ResultSet generatedKeys = prepareStatement.getGeneratedKeys();
//            while (generatedKeys.next()) {
//                System.out.println(generatedKeys.getLong("item_id"));
//            }
//            return (long) executeUpdate;
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

//    public List<ItemsEntity> findAll() {
//        try (Connection connection = ConnectionPoolManager.get();
//             PreparedStatement prepareStatement = connection.prepareStatement(SQL_FIND_ALL)) {
//            ResultSet resultSet = prepareStatement.executeQuery();
//            List<ItemsEntity> itemsList = new ArrayList<>();
//            while (resultSet.next()) {
//                itemsList.add(buildItems(resultSet));
//            }
//            return itemsList;
//        } catch (SQLException e) {
//            throw new RuntimeException();
//        }
//    }

    @Override
    public Optional<ItemsEntity> getById(Long itemId) {
        try (var connection = ConnectionPoolManager.get();
             var prepareStatement = connection.prepareStatement("""
                     SELECT *
                     FROM items
                     WHERE item_id = ?
                     """)) {
            prepareStatement.setLong(1, itemId);
            var resultSet = prepareStatement.executeQuery();
            ItemsEntity entityResult = null;
            while (resultSet.next()) {
                entityResult = buildItems(resultSet);
            }
            return Optional.ofNullable(entityResult);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
//    public Long insert(ItemsEntity entity) {
//        try (Connection connection = ConnectionPoolManager.get();
//             PreparedStatement statement = connection.prepareStatement(SQL_INSERT_STATEMENT,
//                     Statement.RETURN_GENERATED_KEYS)) {
//            statement.setString(1, entity.getModel());
//            statement.setString(2, entity.getBrand());
//            statement.setString(3, entity.getAttributes());
//            statement.setDouble(4, entity.getPrice());
//            statement.setString(5, entity.getCurrency());
//            statement.setInt(6, entity.getQuantity());
//            statement.executeUpdate();
//            ResultSet generatedKeys = statement.getGeneratedKeys();
//            if (generatedKeys.next()) {
//                return generatedKeys.getLong("account_id");
//            }
//        } catch (SQLException e) {
//            SQL_EXCEPTION_LOGGER.addException(e);
//        }
//        return 0l;
//    }

    @Override
    public boolean delete(Long params) {
        try (Connection connection = ConnectionPoolManager.get();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM items
                     WHERE item_id = ?
                     """)) {
            statement.setLong(1, params);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            SQL_EXCEPTION_LOGGER.addException(e);
        }
        return false;
    }

    public Integer changeQuantity(int quantity, long itemId) {
        try (Connection connection = ConnectionPoolManager.get();
             PreparedStatement prepareStatement = connection.prepareStatement("""
                             		UPDATE items
                             		SET quantity=quantity-?
                             WHERE item_id=?
                             		""",
                     Statement.RETURN_GENERATED_KEYS)) {
            prepareStatement.setInt(1, quantity);
            prepareStatement.setLong(2, itemId);
            prepareStatement.executeUpdate();
            ResultSet generatedKeys = prepareStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt("quantity");
            }
            return null;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }

    public List<ItemsEntity> findByBrand(String brand) {
        try (var connection = ConnectionPoolManager.get();
             var prepareStatement = connection.prepareStatement("""
                     SELECT *
                     FROM items
                     WHERE brand = ?
                     """)) {
            prepareStatement.setString(1, brand);
            var resultSet = prepareStatement.executeQuery();
            List<ItemsEntity> itemsList = new ArrayList<>();
            while (resultSet.next()) {
                itemsList.add(buildItems(resultSet));
            }
            return itemsList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ItemsEntity buildItems(ResultSet resultSet) throws SQLException {
        return null;
    }

}
