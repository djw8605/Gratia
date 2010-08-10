def:
	@echo You can run the test directly with:
	@echo "   ./runPurgeTest.sh -p your_port -c -d -l" 
	@echo See
	@echo "   ./runPurgeTest.sh -h" 
	@echo for details
	@echo Or you can run the full build and test with 
	@echo the default options:
	@echo "   gmake test"

build:
	(cd ../../build-scripts; gmake all testtimeout )

test: build
	./runPurgeTest.sh -c -d -l -f -k -m -t

# Target to use when 'just' wanting to re-run the test without rebuilding nor re-installing the collector
onlytest:  
	./runPurgeTest.sh -d -l -f -k -m -t


clean:
	rm -rf MAGIC_VDT_LOCATION ProbeConfig collector-for-test *~ *.validate *.count
