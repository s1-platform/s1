var obj = {
    a:1,
    b:2,
    c:3,
    d:4,
    e:{a:1,b:2}
};
var a = "";
for(var p in obj){
    a+=p+";";
}

assert("a1",s1.contains(a,"a;"));
assert("a2",s1.contains(a,"b;"));
assert("a3",s1.contains(a,"c;"));
assert("a4",s1.contains(a,"d;"));
assert("a5",s1.contains(a,"e;"));

var a = "";
var b = "";
for(var p in obj.e){
    a+=p+";";
    b+=obj.e[p]+",";
}

assert("a2_1",s1.contains(a,"a;"));
assert("a2_2",s1.contains(a,"a;"));
print(b)
assert("b1",s1.contains(b,"1.0,"));
assert("b2",s1.contains(b,"2.0,"));
