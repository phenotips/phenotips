# !!!Edit this value!!!
#
# SD equivalent of the input percentile
# 97% = 1.88079360817072
# 95% = 1.6448536269520015
# 2sd = 2 :)
global z = 1.88079360817072;

arg_list = argv ();
data = str2num(arg_list{1});
global gender = data(1);
global age = data(2);
global m = data(4);
global d1 =  data(3);
global d2 =  data(5);

function y = f (x)
  global m;
  global d1;
  global d2;
  global z;
  y(1) = m * (-z * x(1) * x(2) + 1)^(1/x(2)) - d1;
  y(2) = m * ( z * x(1) * x(2) + 1)^(1/x(2)) - d2;
endfunction

[x, info] = fsolve ("f", [0.1; 1])

l = x(2);
s = x(1);
printf ("%d,%d,%f,%f,%f\n", gender, age, l, m, s);
