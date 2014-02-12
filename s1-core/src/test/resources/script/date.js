var dt = s1.now();
assert("now",dt!=null);

var dt1 = s1.formatDate(dt,"yyyy-MM-dd HH:mm:ss.SSS");

var dt2 = s1.parseDate(dt1,"yyyy-MM-dd HH:mm:ss.SSS");

assert("not null",dt1);
assert("equals",dt==dt2);