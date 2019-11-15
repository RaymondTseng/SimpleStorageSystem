ports='8000 8001 8002 8100 8101 8123 8124 8125 8126 8127 8128'
for p in $ports
	do
		echo $(lsof -t -i :$p)
		kill -kill $(lsof -t -i :$p)
done
