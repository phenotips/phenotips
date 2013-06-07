for line in `cat $1`
do
  octave growth-curves.m $line | tail -n 1
done
