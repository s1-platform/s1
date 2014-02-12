var obj = {
    a:1,
    b:1.2,
    c:"q",
    d:true,
    e:false,
    f:{},
    g:{a:1},
    h:[],
    i:[1,2],
    j:null,
    k:function(){}
};

assert("1",typeof obj.a == "Double")
assert("2",typeof obj.b == "Double")
assert("3",typeof obj.c == "String")
assert("4",typeof obj.d == "Boolean")
assert("5",typeof obj.e == "Boolean")
assert("6",typeof obj.f == "Map")
assert("7",typeof obj.g == "Map")
assert("8",typeof obj.h == "List")
assert("9",typeof obj.i == "List")
assert("10",typeof obj.j == null)
assert("11",typeof obj.k == "Function")