
import Gratia

class Simple:
        "A simple example class"
        i = 12345
        def f(self):
                return 'hello world'

if __name__ == '__main__':
        for i in range(1,5):
            print ''
            print ''
            print ''
            print 'Loop:'
            print i
            print ''
            print ''
            print ''
            Gratia.Initialize()
            Gratia.Reprocess()
