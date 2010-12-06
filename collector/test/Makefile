def:
	@echo You can run the test directly with:
	@echo "   ./runCollectorTest.sh -p your_port -c -d -l" 
	@echo See
	@echo "   ./runCollectorTest.sh -h" 
	@echo for details
	@echo Or you can run the full build and test with 
	@echo the default options:
	@echo "   gmake test"

urCollectorFiles=../../probe/pbs-lsf/pbs-* ../../probe/pbs-lsf/urCollector-src/urCreator ../../probe/pbs-lsf/urCollector-src/*.pl

urCollector:
	(cd ../../build-scripts; gmake urCollector )
	mkdir -p pbs 
	mkdir -p lsf
	cp $(urCollectorFiles) pbs
	cp  ./urCollector.conf.prepared pbs/urCollector.conf
	cp $(urCollectorFiles) lsf
	cat  ./urCollector.conf.prepared | sed -e 's/lrmsType = "pbs"/lrmsType = "lsf"/' > lsf/urCollector.conf

build: urCollector
	(cd ../../build-scripts; gmake all testtimeout urCollector )

test: build
	./runCollectorTest.sh -c -d -l --lsf --pbs -f -k -m -t

# Target to use when 'just' wanting to re-run the test without rebuilding nor re-installing the collector
testonly: onlytest
onlytest:  
	./runCollectorTest.sh -d -l --lsf --pbs -f -k -m -t


clean:
	rm -rf MAGIC_VDT_LOCATION ProbeConfig collector-for-test *~ *.validate *.count
