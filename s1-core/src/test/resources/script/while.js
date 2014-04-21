var a = 0;
while(false) {
    a++;
};

assert("1",a==0);

a = 0;
while(a<10) {
    a++;
}
assert("2",a==10);

a = 0;
while(true) {
    a++;
    if(a==3)
        break;
}
assert("3",a==3);

a = 0;
var i=0;
while(i<100) {
    i++;
    if(i>=10)
        continue;
    a++;
}
assert("4",a==9);