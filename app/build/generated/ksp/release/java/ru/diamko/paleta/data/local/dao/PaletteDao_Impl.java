package ru.diamko.paleta.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import ru.diamko.paleta.data.local.entity.PaletteEntity;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PaletteDao_Impl implements PaletteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PaletteEntity> __insertionAdapterOfPaletteEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  public PaletteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPaletteEntity = new EntityInsertionAdapter<PaletteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `palettes` (`id`,`name`,`colorsJson`,`createdAtIso`,`isSynced`,`pendingAction`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PaletteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getColorsJson());
        statement.bindString(4, entity.getCreatedAtIso());
        final int _tmp = entity.isSynced() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getPendingAction() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getPendingAction());
        }
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM palettes WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM palettes";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE palettes SET isSynced = 1, pendingAction = NULL WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<PaletteEntity> palettes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPaletteEntity.insert(palettes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final PaletteEntity palette, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPaletteEntity.insert(palette);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getVisiblePalettes(final Continuation<? super List<PaletteEntity>> $completion) {
    final String _sql = "SELECT * FROM palettes WHERE pendingAction IS NULL OR pendingAction != 'delete' ORDER BY createdAtIso DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PaletteEntity>>() {
      @Override
      @NonNull
      public List<PaletteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfColorsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "colorsJson");
          final int _cursorIndexOfCreatedAtIso = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtIso");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final int _cursorIndexOfPendingAction = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingAction");
          final List<PaletteEntity> _result = new ArrayList<PaletteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PaletteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpColorsJson;
            _tmpColorsJson = _cursor.getString(_cursorIndexOfColorsJson);
            final String _tmpCreatedAtIso;
            _tmpCreatedAtIso = _cursor.getString(_cursorIndexOfCreatedAtIso);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final String _tmpPendingAction;
            if (_cursor.isNull(_cursorIndexOfPendingAction)) {
              _tmpPendingAction = null;
            } else {
              _tmpPendingAction = _cursor.getString(_cursorIndexOfPendingAction);
            }
            _item = new PaletteEntity(_tmpId,_tmpName,_tmpColorsJson,_tmpCreatedAtIso,_tmpIsSynced,_tmpPendingAction);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingChanges(final Continuation<? super List<PaletteEntity>> $completion) {
    final String _sql = "SELECT * FROM palettes WHERE isSynced = 0 OR pendingAction IS NOT NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PaletteEntity>>() {
      @Override
      @NonNull
      public List<PaletteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfColorsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "colorsJson");
          final int _cursorIndexOfCreatedAtIso = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtIso");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final int _cursorIndexOfPendingAction = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingAction");
          final List<PaletteEntity> _result = new ArrayList<PaletteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PaletteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpColorsJson;
            _tmpColorsJson = _cursor.getString(_cursorIndexOfColorsJson);
            final String _tmpCreatedAtIso;
            _tmpCreatedAtIso = _cursor.getString(_cursorIndexOfCreatedAtIso);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final String _tmpPendingAction;
            if (_cursor.isNull(_cursorIndexOfPendingAction)) {
              _tmpPendingAction = null;
            } else {
              _tmpPendingAction = _cursor.getString(_cursorIndexOfPendingAction);
            }
            _item = new PaletteEntity(_tmpId,_tmpName,_tmpColorsJson,_tmpCreatedAtIso,_tmpIsSynced,_tmpPendingAction);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
