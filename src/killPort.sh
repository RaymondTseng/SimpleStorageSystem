ports='8123 8888 8124 8125 8126'
for p in $ports
	do
		kill -kill $(lsof -t -i :$p)
done