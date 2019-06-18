package cn.schoolwow.quickdao.entity.logic;

import cn.schoolwow.quickdao.annotation.Comment;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

@Comment("数据类型表")
public class DataType {
    private long id;
    private byte _byte;
    private char _char;
    private short _short;
    private int _int;
    private float _float;
    private double _double;
    private Date _date;
    private Time _time;
    private Timestamp _timestamp;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte get_byte() {
        return _byte;
    }

    public void set_byte(byte _byte) {
        this._byte = _byte;
    }

    public char get_char() {
        return _char;
    }

    public void set_char(char _char) {
        this._char = _char;
    }

    public short get_short() {
        return _short;
    }

    public void set_short(short _short) {
        this._short = _short;
    }

    public int get_int() {
        return _int;
    }

    public void set_int(int _int) {
        this._int = _int;
    }

    public float get_float() {
        return _float;
    }

    public void set_float(float _float) {
        this._float = _float;
    }

    public double get_double() {
        return _double;
    }

    public void set_double(double _double) {
        this._double = _double;
    }

    public Date get_date() {
        return _date;
    }

    public void set_date(Date _date) {
        this._date = _date;
    }

    public Time get_time() {
        return _time;
    }

    public void set_time(Time _time) {
        this._time = _time;
    }

    public Timestamp get_timestamp() {
        return _timestamp;
    }

    public void set_timestamp(Timestamp _timestamp) {
        this._timestamp = _timestamp;
    }
}
