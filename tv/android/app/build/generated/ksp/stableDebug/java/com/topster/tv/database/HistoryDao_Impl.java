package com.topster.tv.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HistoryDao_Impl implements HistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HistoryEntity> __insertionAdapterOfHistoryEntity;

  private final EntityDeletionOrUpdateAdapter<HistoryEntity> __updateAdapterOfHistoryEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteHistoryEntry;

  private final SharedSQLiteStatement __preparedStmtOfClearAllHistory;

  public HistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHistoryEntity = new EntityInsertionAdapter<HistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `watch_history` (`rowId`,`mediaId`,`title`,`type`,`url`,`posterImage`,`episodeId`,`episodeTitle`,`seasonNumber`,`episodeNumber`,`position`,`duration`,`percentWatched`,`completed`,`lastWatched`,`firstWatched`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HistoryEntity entity) {
        statement.bindLong(1, entity.getRowId());
        statement.bindString(2, entity.getMediaId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getType());
        statement.bindString(5, entity.getUrl());
        if (entity.getPosterImage() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getPosterImage());
        }
        if (entity.getEpisodeId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEpisodeId());
        }
        if (entity.getEpisodeTitle() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getEpisodeTitle());
        }
        if (entity.getSeasonNumber() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getSeasonNumber());
        }
        if (entity.getEpisodeNumber() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getEpisodeNumber());
        }
        statement.bindLong(11, entity.getPosition());
        statement.bindLong(12, entity.getDuration());
        statement.bindDouble(13, entity.getPercentWatched());
        final int _tmp = entity.getCompleted() ? 1 : 0;
        statement.bindLong(14, _tmp);
        statement.bindLong(15, entity.getLastWatched());
        statement.bindLong(16, entity.getFirstWatched());
      }
    };
    this.__updateAdapterOfHistoryEntity = new EntityDeletionOrUpdateAdapter<HistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `watch_history` SET `rowId` = ?,`mediaId` = ?,`title` = ?,`type` = ?,`url` = ?,`posterImage` = ?,`episodeId` = ?,`episodeTitle` = ?,`seasonNumber` = ?,`episodeNumber` = ?,`position` = ?,`duration` = ?,`percentWatched` = ?,`completed` = ?,`lastWatched` = ?,`firstWatched` = ? WHERE `rowId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HistoryEntity entity) {
        statement.bindLong(1, entity.getRowId());
        statement.bindString(2, entity.getMediaId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getType());
        statement.bindString(5, entity.getUrl());
        if (entity.getPosterImage() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getPosterImage());
        }
        if (entity.getEpisodeId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEpisodeId());
        }
        if (entity.getEpisodeTitle() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getEpisodeTitle());
        }
        if (entity.getSeasonNumber() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getSeasonNumber());
        }
        if (entity.getEpisodeNumber() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getEpisodeNumber());
        }
        statement.bindLong(11, entity.getPosition());
        statement.bindLong(12, entity.getDuration());
        statement.bindDouble(13, entity.getPercentWatched());
        final int _tmp = entity.getCompleted() ? 1 : 0;
        statement.bindLong(14, _tmp);
        statement.bindLong(15, entity.getLastWatched());
        statement.bindLong(16, entity.getFirstWatched());
        statement.bindLong(17, entity.getRowId());
      }
    };
    this.__preparedStmtOfDeleteHistoryEntry = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM watch_history WHERE mediaId = ? AND episodeId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllHistory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM watch_history";
        return _query;
      }
    };
  }

  @Override
  public Object insertHistory(final HistoryEntity history,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHistoryEntity.insertAndReturnId(history);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateHistory(final HistoryEntity history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHistoryEntity.handle(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteHistoryEntry(final String mediaId, final String episodeId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteHistoryEntry.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, mediaId);
        _argIndex = 2;
        if (episodeId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, episodeId);
        }
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
          __preparedStmtOfDeleteHistoryEntry.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllHistory(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllHistory.acquire();
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
          __preparedStmtOfClearAllHistory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HistoryEntity>> getRecentHistory(final int limit) {
    final String _sql = "SELECT * FROM watch_history ORDER BY lastWatched DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_history"}, new Callable<List<HistoryEntity>>() {
      @Override
      @NonNull
      public List<HistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRowId = CursorUtil.getColumnIndexOrThrow(_cursor, "rowId");
          final int _cursorIndexOfMediaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfPosterImage = CursorUtil.getColumnIndexOrThrow(_cursor, "posterImage");
          final int _cursorIndexOfEpisodeId = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeId");
          final int _cursorIndexOfEpisodeTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeTitle");
          final int _cursorIndexOfSeasonNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "seasonNumber");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfPercentWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "percentWatched");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfLastWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "lastWatched");
          final int _cursorIndexOfFirstWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "firstWatched");
          final List<HistoryEntity> _result = new ArrayList<HistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryEntity _item;
            final long _tmpRowId;
            _tmpRowId = _cursor.getLong(_cursorIndexOfRowId);
            final String _tmpMediaId;
            _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpPosterImage;
            if (_cursor.isNull(_cursorIndexOfPosterImage)) {
              _tmpPosterImage = null;
            } else {
              _tmpPosterImage = _cursor.getString(_cursorIndexOfPosterImage);
            }
            final String _tmpEpisodeId;
            if (_cursor.isNull(_cursorIndexOfEpisodeId)) {
              _tmpEpisodeId = null;
            } else {
              _tmpEpisodeId = _cursor.getString(_cursorIndexOfEpisodeId);
            }
            final String _tmpEpisodeTitle;
            if (_cursor.isNull(_cursorIndexOfEpisodeTitle)) {
              _tmpEpisodeTitle = null;
            } else {
              _tmpEpisodeTitle = _cursor.getString(_cursorIndexOfEpisodeTitle);
            }
            final Integer _tmpSeasonNumber;
            if (_cursor.isNull(_cursorIndexOfSeasonNumber)) {
              _tmpSeasonNumber = null;
            } else {
              _tmpSeasonNumber = _cursor.getInt(_cursorIndexOfSeasonNumber);
            }
            final Integer _tmpEpisodeNumber;
            if (_cursor.isNull(_cursorIndexOfEpisodeNumber)) {
              _tmpEpisodeNumber = null;
            } else {
              _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            }
            final long _tmpPosition;
            _tmpPosition = _cursor.getLong(_cursorIndexOfPosition);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final float _tmpPercentWatched;
            _tmpPercentWatched = _cursor.getFloat(_cursorIndexOfPercentWatched);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final long _tmpLastWatched;
            _tmpLastWatched = _cursor.getLong(_cursorIndexOfLastWatched);
            final long _tmpFirstWatched;
            _tmpFirstWatched = _cursor.getLong(_cursorIndexOfFirstWatched);
            _item = new HistoryEntity(_tmpRowId,_tmpMediaId,_tmpTitle,_tmpType,_tmpUrl,_tmpPosterImage,_tmpEpisodeId,_tmpEpisodeTitle,_tmpSeasonNumber,_tmpEpisodeNumber,_tmpPosition,_tmpDuration,_tmpPercentWatched,_tmpCompleted,_tmpLastWatched,_tmpFirstWatched);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<HistoryEntity>> getIncompleteHistory() {
    final String _sql = "SELECT * FROM watch_history WHERE completed = 0 ORDER BY lastWatched DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watch_history"}, new Callable<List<HistoryEntity>>() {
      @Override
      @NonNull
      public List<HistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRowId = CursorUtil.getColumnIndexOrThrow(_cursor, "rowId");
          final int _cursorIndexOfMediaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfPosterImage = CursorUtil.getColumnIndexOrThrow(_cursor, "posterImage");
          final int _cursorIndexOfEpisodeId = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeId");
          final int _cursorIndexOfEpisodeTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeTitle");
          final int _cursorIndexOfSeasonNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "seasonNumber");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfPercentWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "percentWatched");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfLastWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "lastWatched");
          final int _cursorIndexOfFirstWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "firstWatched");
          final List<HistoryEntity> _result = new ArrayList<HistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryEntity _item;
            final long _tmpRowId;
            _tmpRowId = _cursor.getLong(_cursorIndexOfRowId);
            final String _tmpMediaId;
            _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpPosterImage;
            if (_cursor.isNull(_cursorIndexOfPosterImage)) {
              _tmpPosterImage = null;
            } else {
              _tmpPosterImage = _cursor.getString(_cursorIndexOfPosterImage);
            }
            final String _tmpEpisodeId;
            if (_cursor.isNull(_cursorIndexOfEpisodeId)) {
              _tmpEpisodeId = null;
            } else {
              _tmpEpisodeId = _cursor.getString(_cursorIndexOfEpisodeId);
            }
            final String _tmpEpisodeTitle;
            if (_cursor.isNull(_cursorIndexOfEpisodeTitle)) {
              _tmpEpisodeTitle = null;
            } else {
              _tmpEpisodeTitle = _cursor.getString(_cursorIndexOfEpisodeTitle);
            }
            final Integer _tmpSeasonNumber;
            if (_cursor.isNull(_cursorIndexOfSeasonNumber)) {
              _tmpSeasonNumber = null;
            } else {
              _tmpSeasonNumber = _cursor.getInt(_cursorIndexOfSeasonNumber);
            }
            final Integer _tmpEpisodeNumber;
            if (_cursor.isNull(_cursorIndexOfEpisodeNumber)) {
              _tmpEpisodeNumber = null;
            } else {
              _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            }
            final long _tmpPosition;
            _tmpPosition = _cursor.getLong(_cursorIndexOfPosition);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final float _tmpPercentWatched;
            _tmpPercentWatched = _cursor.getFloat(_cursorIndexOfPercentWatched);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final long _tmpLastWatched;
            _tmpLastWatched = _cursor.getLong(_cursorIndexOfLastWatched);
            final long _tmpFirstWatched;
            _tmpFirstWatched = _cursor.getLong(_cursorIndexOfFirstWatched);
            _item = new HistoryEntity(_tmpRowId,_tmpMediaId,_tmpTitle,_tmpType,_tmpUrl,_tmpPosterImage,_tmpEpisodeId,_tmpEpisodeTitle,_tmpSeasonNumber,_tmpEpisodeNumber,_tmpPosition,_tmpDuration,_tmpPercentWatched,_tmpCompleted,_tmpLastWatched,_tmpFirstWatched);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getHistoryEntry(final String mediaId, final String episodeId,
      final Continuation<? super HistoryEntity> $completion) {
    final String _sql = "SELECT * FROM watch_history WHERE mediaId = ? AND episodeId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, mediaId);
    _argIndex = 2;
    if (episodeId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, episodeId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HistoryEntity>() {
      @Override
      @Nullable
      public HistoryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRowId = CursorUtil.getColumnIndexOrThrow(_cursor, "rowId");
          final int _cursorIndexOfMediaId = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfPosterImage = CursorUtil.getColumnIndexOrThrow(_cursor, "posterImage");
          final int _cursorIndexOfEpisodeId = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeId");
          final int _cursorIndexOfEpisodeTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeTitle");
          final int _cursorIndexOfSeasonNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "seasonNumber");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfPercentWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "percentWatched");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfLastWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "lastWatched");
          final int _cursorIndexOfFirstWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "firstWatched");
          final HistoryEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpRowId;
            _tmpRowId = _cursor.getLong(_cursorIndexOfRowId);
            final String _tmpMediaId;
            _tmpMediaId = _cursor.getString(_cursorIndexOfMediaId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpPosterImage;
            if (_cursor.isNull(_cursorIndexOfPosterImage)) {
              _tmpPosterImage = null;
            } else {
              _tmpPosterImage = _cursor.getString(_cursorIndexOfPosterImage);
            }
            final String _tmpEpisodeId;
            if (_cursor.isNull(_cursorIndexOfEpisodeId)) {
              _tmpEpisodeId = null;
            } else {
              _tmpEpisodeId = _cursor.getString(_cursorIndexOfEpisodeId);
            }
            final String _tmpEpisodeTitle;
            if (_cursor.isNull(_cursorIndexOfEpisodeTitle)) {
              _tmpEpisodeTitle = null;
            } else {
              _tmpEpisodeTitle = _cursor.getString(_cursorIndexOfEpisodeTitle);
            }
            final Integer _tmpSeasonNumber;
            if (_cursor.isNull(_cursorIndexOfSeasonNumber)) {
              _tmpSeasonNumber = null;
            } else {
              _tmpSeasonNumber = _cursor.getInt(_cursorIndexOfSeasonNumber);
            }
            final Integer _tmpEpisodeNumber;
            if (_cursor.isNull(_cursorIndexOfEpisodeNumber)) {
              _tmpEpisodeNumber = null;
            } else {
              _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            }
            final long _tmpPosition;
            _tmpPosition = _cursor.getLong(_cursorIndexOfPosition);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final float _tmpPercentWatched;
            _tmpPercentWatched = _cursor.getFloat(_cursorIndexOfPercentWatched);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final long _tmpLastWatched;
            _tmpLastWatched = _cursor.getLong(_cursorIndexOfLastWatched);
            final long _tmpFirstWatched;
            _tmpFirstWatched = _cursor.getLong(_cursorIndexOfFirstWatched);
            _result = new HistoryEntity(_tmpRowId,_tmpMediaId,_tmpTitle,_tmpType,_tmpUrl,_tmpPosterImage,_tmpEpisodeId,_tmpEpisodeTitle,_tmpSeasonNumber,_tmpEpisodeNumber,_tmpPosition,_tmpDuration,_tmpPercentWatched,_tmpCompleted,_tmpLastWatched,_tmpFirstWatched);
          } else {
            _result = null;
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
  public Object getHistoryCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM watch_history";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
