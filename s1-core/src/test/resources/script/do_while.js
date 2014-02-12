var a = 0;
do {
    a++;
} while(false);

assert("1",a==1);

a = 0;
do {
    a++;
} while(a<10);
assert("2",a==10);

print(a);
a = 0;
do {
    a++;
    if(a==3)
        break;
} while(true);
print(a);
assert("3",a==3);

var i=0;
a = 0;
do {
    i++;
    if(i>=10)
        continue;
    a++;
} while(i<100);
print(a);
assert("4",a==9);