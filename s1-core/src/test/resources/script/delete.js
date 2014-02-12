var obj = {
    a:1,
    b:2,
    c:{"d-1":"a",c:2}
};
print(obj);
print(obj.c);
print(obj.c['d-1']);
assert("d-1",obj.c['d-1']=="a")

print("before delete");
delete obj.a;
delete obj.c['d-1'];
print("deleted");

assert("d-1 null",!obj.c['d-1'])

var a = "";
for(var p in obj){
    a+=p+";";
}
print(a);
assert("b;",s1.contains(a,"b;"));
assert("c;",s1.contains(a,"c;"));
assert("a;",!s1.contains(a,"a;"));