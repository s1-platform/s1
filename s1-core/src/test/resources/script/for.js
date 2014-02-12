var a = 0;
for(var i=0;i<0;i++) {
    a++;
};

assert("1",a==0);
assert("i",i==null);
assert("i2",!i);

a = 0;
for(var i=0;i<10;i++) {
    a++;
}
assert("2",a==10);

a = 0;
for(var i=0;i<1000;i++) {
    a++;
    if(a==3)
        break;
}
assert("3",a==3);

a = 0;
for(var i=0;i<100;i++) {
    if(a>=10)
        continue;
    a++;
}
assert("4",a==10);